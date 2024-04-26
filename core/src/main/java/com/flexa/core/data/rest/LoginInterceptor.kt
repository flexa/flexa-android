package com.flexa.core.data.rest

import android.os.Build
import com.flexa.BuildConfig
import com.flexa.core.Flexa
import com.flexa.core.data.data.AppInfoProvider
import com.flexa.core.data.data.TokenProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.UUID


internal class LoginInterceptor(
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
                    "Flexa/${BuildConfig.SPEND_SDK_VERSION} " +
                    "$appPackageName/$appVersion"
        HeadersBundle(
            appName = appName,
            appVersion = appVersion,
            userAgent = userAgent
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
        val request = newRequestWithAccessToken(chain.request(), token)
        var response = chain.proceed(request)
        return response
    }

    private fun newRequestWithAccessToken(
        request: Request,
        token: String
    ): Request {
        val tokenBase64 = android.util.Base64.encodeToString(
            ":${token}".toByteArray(), android.util.Base64.NO_WRAP
        )

        return request.newBuilder()
            .header("Accept", "application/vnd.flexa+json")
            .header("Flexa-App", headersBundle.appName)
            .header("Flexa-Version", headersBundle.appVersion)
            .header("User-Agent", headersBundle.userAgent)
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic $tokenBase64")
            .header("client-trace-id", UUID.randomUUID().toString())
            .build()
    }
}
