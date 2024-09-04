package com.flexa.core.data.rest

import android.os.Build
import android.util.Log
import com.flexa.BuildConfig
import com.flexa.core.Flexa
import com.flexa.core.data.data.AppInfoProvider
import com.flexa.core.data.data.TokenProvider
import com.flexa.core.minutesBetween
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection
import java.time.Instant
import java.util.UUID


internal const val MINIMUM_REFRESH_MINUTES = 5L

internal class HeadersInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    private val headersBundle by lazy(LazyThreadSafetyMode.NONE) {
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
            appVersion = appVersion,
            userAgent = userAgent
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val tokenExpiration = tokenProvider.getTokenExpiration()
        val tokenExpirationMinutes = Instant.now().minutesBetween(tokenExpiration)
        Log.d("TAG", "intercept: tokenExpiration > $tokenExpirationMinutes minutes")
        val token = if (tokenExpirationMinutes <= MINIMUM_REFRESH_MINUTES)
            tokenProvider.getRefreshToken(headersBundle)
        else tokenProvider.getToken()
        val request = newRequestWithAccessToken(chain.request(), token)
        var response = chain.proceed(request)

        if (response.code == HttpURLConnection.HTTP_FORBIDDEN ||
            response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            response.close()
            synchronized(this) {
                val newAccessToken = tokenProvider.getRefreshToken(headersBundle)
                response = chain.proceed(newRequestWithAccessToken(request, newAccessToken))
            }
        }
        return response
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
            .header("Accept", "application/vnd.flexa+json")
            .header("Flexa-App", headersBundle.appName)
            .header("User-Agent", headersBundle.userAgent)
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic $tokenBase64")
            .header("client-trace-id", UUID.randomUUID().toString())
            .build()
    }
}
