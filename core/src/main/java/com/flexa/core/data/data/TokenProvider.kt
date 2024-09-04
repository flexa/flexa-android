package com.flexa.core.data.data

import android.content.Intent
import android.util.Log
import com.flexa.BuildConfig
import com.flexa.core.Flexa
import com.flexa.core.data.rest.HeadersBundle
import com.flexa.core.data.rest.RestRepository
import com.flexa.core.data.rest.RestRepository.Companion.json
import com.flexa.core.data.storage.SecuredPreferences
import com.flexa.core.domain.data.ITokenProvider
import com.flexa.core.entity.TokenPatch
import com.flexa.core.shared.FlexaConstants
import com.flexa.core.shared.FlexaConstants.Companion.EMPTY
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

private const val TOKEN_ERROR_COUNTER = 3

internal class TokenProvider(
    private val preferences: SecuredPreferences,
    private val tokenKey: String
) : ITokenProvider {

    private val client by lazy {
        OkHttpClient().newBuilder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (BuildConfig.DEBUG) {
                    true -> HttpLoggingInterceptor.Level.BODY
                    else -> HttpLoggingInterceptor.Level.NONE
                }
            }
            ).build()
    }
    private var countDownLatch = AtomicReference<CountDownLatch>()
    private var errorCounter = 0

    override fun getTokenExpiration(): Long =
        preferences.getLongSynchronously(FlexaConstants.TOKEN_EXPIRATION)

    override fun getToken(): String =
        preferences.getStringSynchronously(tokenKey) ?: EMPTY

    override fun setToken(token: String?) =
        preferences.edit()
            .putString(tokenKey, token)
            .apply()

    override fun getRefreshToken(headersBundle: HeadersBundle): String {
        if (countDownLatch.get() != null) {
            countDownLatch.get()?.await()
            val token = getToken()
            Log.d(TokenProvider::class.java.simpleName, "refreshed token Waiting ⏳: $token")
            return token
        } else {
            countDownLatch.set(CountDownLatch(1))
            Log.d(TokenProvider::class.java.simpleName, "refresh: ${Thread.currentThread().name}")

            val url = HttpUrl.Builder()
                .scheme(RestRepository.SCHEME)
                .host(RestRepository.host)
                .addPathSegment("tokens").build()

            val verifier = preferences.getStringSynchronously(FlexaConstants.VERIFIER)
            val newVerifier = PikSeeProvider.getCodeVerifier()
            val challenge = PikSeeProvider.getCodeChallenge(newVerifier)

            val body = buildJsonObject {
                put("challenge", challenge)
                put("verifier", verifier)
            }.run { toString().toRequestBody(RestRepository.mediaType) }

            val oldToken = getToken()
            val tokenBase64 = android.util.Base64.encodeToString(
                ":${oldToken}".toByteArray(), android.util.Base64.NO_WRAP
            )

            val request: Request = Request.Builder().url(url)
                .header("Accept", "application/vnd.flexa+json")
                .header("Flexa-App", headersBundle.appName)
                .header("Flexa-Version", headersBundle.appVersion)
                .header("User-Agent", headersBundle.userAgent)
                .header("Authorization", "Basic $tokenBase64")
                .header("client-trace-id", UUID.randomUUID().toString())
                .post(body).build()

            val token = try {
                val response = client.newCall(request).execute()
                val raw = response.body?.string().toString()
                val tokenResponse = try {
                    json.decodeFromString<TokenPatch>(raw)
                } catch (e: Exception) {
                    TokenPatch("", "", "", 0)
                }
                val token = tokenResponse.value
                if (token.isNotBlank()) {
                    errorCounter = 0
                    preferences.saveStringSynchronous(FlexaConstants.VERIFIER, newVerifier)
                    preferences.edit()
                        .putLong(FlexaConstants.TOKEN_EXPIRATION, tokenResponse.expiration).apply()
                    setToken(token)
                    Flexa.context?.let {
                        val intent = Intent(preferences.getPublishableKey())
                        intent.putExtra(FlexaConstants.TOKEN, true)
                        it.sendBroadcast(intent)
                    }
                } else {
                    if (++errorCounter >= TOKEN_ERROR_COUNTER) {
                        Flexa.context?.let {
                            val intent = Intent(preferences.getPublishableKey())
                            intent.putExtra(FlexaConstants.TOKEN, false)
                            it.sendBroadcast(intent)
                        }
                    }
                }
                token
            } catch (e: Exception) {
                Log.e("TAG", "getRefreshToken: ", e)
                ""
            }

            try {
                Log.d(TokenProvider::class.java.simpleName, "refreshed token: \uD83C\uDFC1: $token")
                return token
            } finally {
                countDownLatch.getAndSet(null)?.countDown()
            }
        }
    }
}
