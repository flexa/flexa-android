package com.flexa.core.data.rest

import com.flexa.BuildConfig
import com.flexa.core.data.data.TokenProvider
import com.flexa.core.data.storage.SecuredPreferences
import com.flexa.core.shared.FlexaConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

internal class OkHttpProvider(
    private val preferences: SecuredPreferences
) {

    internal val loginClient: OkHttpClient
        get() {
            val tokenProvider = TokenProvider(preferences, FlexaConstants.PUBLISHABLE_KEY)
            return OkHttpClient().newBuilder()
                .addInterceptor(LoginInterceptor(tokenProvider))
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = when (BuildConfig.DEBUG) {
                        true -> HttpLoggingInterceptor.Level.BODY
                        else -> HttpLoggingInterceptor.Level.NONE
                    }
                }
                )
                .build()
        }

    internal val client by lazy {
        val tokenProvider = tokenProvider
        OkHttpClient().newBuilder()
            .addInterceptor(HeadersInterceptor(tokenProvider))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (BuildConfig.DEBUG) {
                    true -> HttpLoggingInterceptor.Level.BODY
                    else -> HttpLoggingInterceptor.Level.NONE
                }
            }
            )
            .build()
    }
    internal val sseClient by lazy {
        val tokenProvider = tokenProvider
        OkHttpClient().newBuilder()
            .addInterceptor(HeadersSSEInterceptor(tokenProvider))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (BuildConfig.DEBUG) {
                    true -> HttpLoggingInterceptor.Level.HEADERS
                    else -> HttpLoggingInterceptor.Level.NONE
                }
            }
            )
            .build()
    }
    internal val tokenProvider by lazy {
        TokenProvider(preferences, FlexaConstants.TOKEN)
    }
}