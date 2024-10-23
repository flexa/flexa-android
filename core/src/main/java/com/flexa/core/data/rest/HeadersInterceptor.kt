package com.flexa.core.data.rest

import android.content.Intent
import android.os.Build
import com.flexa.BuildConfig
import com.flexa.core.Flexa
import com.flexa.core.data.data.AppInfoProvider
import com.flexa.core.data.data.TokenProvider
import com.flexa.core.data.rest.RestRepository.Companion.json
import com.flexa.core.minutesBetween
import com.flexa.core.shared.FlexaConstants
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection
import java.time.Instant
import java.util.UUID


internal const val MINIMUM_REFRESH_MINUTES = 5

internal class HeadersInterceptor(
    private val tokenProvider: TokenProvider,
    private val keepAlive: Boolean = false
) : Interceptor {

    private val headersBundle by lazy(LazyThreadSafetyMode.NONE) {
        val releaseDate = BuildConfig.RELEASE_DATE
        val appName =
            Flexa.context?.run { AppInfoProvider.getAppName(this) } ?: "Inaccessible"
        val appVersion =
            Flexa.context?.run { AppInfoProvider.getAppVersion(this) } ?: "Inaccessible"
        val appPackageName =
            Flexa.context?.run { AppInfoProvider.getAppPackageName(this) } ?: "Inaccessible"
        val deviceModel = Build.MODEL
        val deviceManufacturer = Build.MANUFACTURER
        val userAgent =
            "$deviceManufacturer $deviceModel/${Build.VERSION.SDK_INT}(${Build.VERSION.RELEASE}) " +
                    "Spend/${BuildConfig.SPEND_SDK_VERSION} " +
                    "$appPackageName/$appVersion"
        HeadersBundle(
            appName = appName,
            version = releaseDate,
            userAgent = userAgent
        )
    }
    private val mutex = tokenProvider.mutex

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        mutex.withLock {
            val tokenExpiration = tokenProvider.getTokenExpiration()
            val tokenExpirationMinutes = Instant.now().minutesBetween(tokenExpiration)

            if (tokenExpirationMinutes <= MINIMUM_REFRESH_MINUTES) {
                tokenProvider.getRefreshToken(headersBundle)
            }
        }
        val token = tokenProvider.getToken()
        val request = newRequestWithAccessToken(chain.request(), token)
        var response = chain.proceed(request)

        checkResponse(response)

        if (response.code == HttpURLConnection.HTTP_FORBIDDEN ||
            response.code == HttpURLConnection.HTTP_UNAUTHORIZED
        ) {
            response.close()
            val newAccessToken = tokenProvider.getRefreshToken(headersBundle)
            response = chain.proceed(newRequestWithAccessToken(request, newAccessToken))
        }

        response
    }

    private fun checkResponse(response: Response) {
        if (!response.isSuccessful) {
            runCatching {
                isRestricted(response)
            }.onSuccess { restricted ->
                Flexa._canSpend.value = !restricted
                if (restricted) {
                    Flexa.context?.let {
                        val intent = Intent(tokenProvider.preferences.getPublishableKey())
                        intent.putExtra(FlexaConstants.RESTRICTED_REGION, restricted)
                        it.sendBroadcast(intent)
                    }
                }
            }
        } else {
            Flexa._canSpend.value = true
        }
    }

    @Throws(OutOfMemoryError::class, SerializationException::class, IllegalArgumentException::class)
    private fun isRestricted(response: Response): Boolean {
        val raw = response.body?.string().toString()
        val jsonResponse = json.parseToJsonElement(raw)
        val res = if ("error" in jsonResponse.jsonObject) {
            val errorObject = jsonResponse.jsonObject["error"]
            val type = errorObject?.jsonObject?.get("type")?.jsonPrimitive?.contentOrNull
            type == "region_not_supported"
        } else {
            false
        }
        return res
    }

    private fun newRequestWithAccessToken(
        request: Request,
        token: String
    ): Request {
        val tokenText = ":${token}"
        val tokenBase64 = android.util.Base64.encodeToString(
            tokenText.toByteArray(), android.util.Base64.NO_WRAP
        )

        return request.newBuilder()
            .header("Flexa-App", headersBundle.appName)
            .header("Flexa-Version", headersBundle.version)
            .header("User-Agent", headersBundle.userAgent)
            .header("Authorization", "Basic $tokenBase64")
            .header("client-trace-id", UUID.randomUUID().toString())
            .run {
                if (!keepAlive) {
                    this.header("Accept", "application/vnd.flexa+json")
                } else {
                    this.header("Accept", "text/event-stream")
                        .header("Cache-Control", "no-cache")
                        .header("Connection", "keep-alive")
                }
            }
            .build()
    }
}
