package com.flexa.core.data.rest

import android.util.Log
import com.flexa.BuildConfig
import com.flexa.core.data.storage.SecuredPreferences
import com.flexa.core.domain.rest.IRestRepository
import com.flexa.core.entity.Account
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.CommerceSessionEvent
import com.flexa.core.entity.PutAppAccountsResponse
import com.flexa.core.entity.Quote
import com.flexa.core.entity.TokenPatch
import com.flexa.core.entity.TokensResponse
import com.flexa.core.shared.AppAccount
import com.flexa.core.shared.Asset
import com.flexa.core.shared.AssetsResponse
import com.flexa.core.shared.Brand
import com.flexa.core.shared.BrandsResponse
import com.flexa.core.shared.FlexaConstants
import com.flexa.core.toJsonObject
import com.flexa.identity.create_id.AccountsRequest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.EMPTY_REQUEST
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class RestRepository(
    private val preferences: SecuredPreferences,
) : IRestRepository {

    companion object {
        const val SCHEME = "https"
        val host = when (BuildConfig.USE_DEBUG_API_HOST.toBoolean()) {
            true -> BuildConfig.DEBUG_API_HOST
            else -> "api.flexa.co"
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
    }

    private val okHttpProvider = OkHttpProvider(preferences)

    override suspend fun tokens(email: String, challenge: String): TokensResponse =
        suspendCancellableCoroutine { cont ->
            val url = HttpUrl.Builder().scheme(SCHEME).host(host)
                .addPathSegment("tokens")
                .build()

            val body = buildJsonObject {
                val deviceId =
                    preferences.getStringSynchronously(FlexaConstants.UNIQUE_IDENTIFIER) ?: ""
                put("challenge", challenge)
                put("device_id", deviceId)
                put("device_model", "Android")
                put("email", email)
            }.run { toString().toRequestBody(mediaType) }

            val request: Request = Request.Builder()
                .url(url).post(body).build()

            runCatching { okHttpProvider.loginClient.newCall(request).execute() }
                .onSuccess { response ->
                    try {
                        val raw = response.body?.string().toString()
                        val jsonResponse = json.parseToJsonElement(raw)
                        when {
                            "id" in jsonResponse.jsonObject
                                    && "status" in jsonResponse.jsonObject -> {
                                val id = jsonResponse.jsonObject["id"]?.jsonPrimitive?.contentOrNull
                                val status =
                                    jsonResponse.jsonObject["status"]?.jsonPrimitive?.contentOrNull
                                if (id != null && status != null) {
                                    cont.resume(TokensResponse.Success(id, status))
                                } else {
                                    cont.resumeWithException(Exception(raw))
                                }
                            }

                            "error" in jsonResponse.jsonObject -> {
                                val error = jsonResponse.jsonObject["error"]
                                val code =
                                    error?.jsonObject?.get("code")?.jsonPrimitive?.contentOrNull
                                val message =
                                    error?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                                if (code != null && message != null) {
                                    cont.resume(TokensResponse.Error(code, message))
                                } else {
                                    cont.resumeWithException(Exception(raw))
                                }
                            }

                            else -> {
                                cont.resumeWithException(Exception(raw))
                            }
                        }

                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                }
                .onFailure { ex -> cont.resumeWithException(ex) }
        }

    override suspend fun accounts(request: AccountsRequest): Int =
        suspendCancellableCoroutine { cont ->
            val url = HttpUrl.Builder().scheme(SCHEME).host(host)
                .addPathSegment("accounts")
                .build()

            val body = json.encodeToString(request).toRequestBody(mediaType)

            val req: Request = Request.Builder()
                .url(url).post(body).build()

            runCatching { okHttpProvider.loginClient.newCall(req).execute() }
                .onSuccess { response -> cont.resume(response.code) }
                .onFailure { ex -> cont.resumeWithException(ex) }
        }


    override suspend fun patchTokens(
        id: String,
        verifier: String,
        challenge: String,
        code: String?,
        link: String?,
    ): TokenPatch = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("tokens")
            .addPathSegment(id)
            .build()

        val body = buildJsonObject {
            put("challenge", challenge)
            put("verifier", verifier)
            code?.let { data -> put("code", data) }
            link?.let { data -> put("link", data) }
        }.run { toString().toRequestBody(mediaType) }

        val request: Request = Request.Builder()
            .url(url).patch(body).build()

        runCatching { okHttpProvider.loginClient.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching {
                        val raw = response.body?.string().toString()
                        json.decodeFromString<TokenPatch>(raw)
                    }.fold(
                        onSuccess = { dto -> it.resume(dto) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun putAccounts(accounts: List<AppAccount>): PutAppAccountsResponse =
        suspendCancellableCoroutine {
            val url = HttpUrl.Builder()
                .scheme(SCHEME).host(host)
                .addPathSegment("accounts")
                .addPathSegment("me")
                .addPathSegment("app_accounts")
                .build()

            val body = accounts.toJsonObject()
                .run { toString().toRequestBody(mediaType) }

            val request: Request = Request.Builder().url(url)
                .put(body).build()

            runCatching { okHttpProvider.client.newCall(request).execute() }
                .fold(
                    onSuccess = { response ->
                        runCatching {
                            val raw = response.body?.string().toString()
                            val jsonElement = json.parseToJsonElement(raw)
                            val hasMore = jsonElement.jsonObject["has_more"].toString().toBoolean()
                            val data = jsonElement.jsonObject["data"]
                            val accData = if (data !is JsonNull) {
                                data!!
                            } else buildJsonArray { }
                            val dto =
                                json.decodeFromJsonElement<List<com.flexa.core.entity.AppAccount>>(
                                    accData
                                )
                            val date = response.header("date", null) ?: ""
                            PutAppAccountsResponse(hasMore = hasMore, accounts = dto, date = date)
                        }.fold(
                            onSuccess = { dto -> it.resume(dto) },
                            onFailure = { ex ->
                                Log.e(null, "putAccounts:>>> ", ex)
                                it.resumeWithException(ex)
                            }
                        )
                    },
                    onFailure = { ex -> it.resumeWithException(ex) }
                )
        }

    override suspend fun getAssets(pageSize: Int, startingAfter: String?): AssetsResponse =
        suspendCancellableCoroutine {
            val url = HttpUrl.Builder()
                .scheme(SCHEME).host(host)
                .addPathSegment("assets").run {
                    if (startingAfter != null) {
                        addEncodedQueryParameter("starting_after", startingAfter)
                    } else this
                }
                .addEncodedQueryParameter("limit", pageSize.toString())
                .build()

            val request: Request = Request.Builder().url(url).get().build()

            runCatching { okHttpProvider.client.newCall(request).execute() }
                .fold(
                    onSuccess = { response ->
                        runCatching {
                            val raw = response.body?.string().toString()
                            val jsonElement = json.parseToJsonElement(raw)
                            val hasMore = jsonElement.jsonObject["has_more"].toString().toBoolean()
                            val dataObject =
                                jsonElement.jsonObject["data"] ?: JsonObject(emptyMap())
                            val data = json.decodeFromJsonElement<List<Asset>>(dataObject)
                            val lastId = if (hasMore) data.lastOrNull()?.id else null
                            val dto = AssetsResponse(startingAfter = lastId, data = data)
                            dto
                        }.fold(
                            onSuccess = { dto -> it.resume(dto) },
                            onFailure = { ex -> it.resumeWithException(ex) }
                        )
                    },
                    onFailure = { ex -> it.resumeWithException(ex) }
                )

        }

    override suspend fun getAssetById(assetId: String): Asset = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("assets")
            .addEncodedPathSegment(assetId)
            .build()

        val request: Request = Request.Builder().url(url).get().build()

        runCatching { okHttpProvider.client.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching {
                        val raw = response.body?.string().toString()
                        val dto = json.decodeFromString<Asset>(raw)
                        dto
                    }.fold(
                        onSuccess = { dto -> it.resume(dto) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun getAccount(): Account = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("accounts")
            .addPathSegment("me")
            .build()

        val request: Request = Request.Builder().url(url).get().build()

        runCatching { okHttpProvider.client.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching {
                        val raw = response.body?.string().toString()
                        json.decodeFromString<Account>(raw)
                    }.fold(
                        onSuccess = { dto -> it.resume(dto) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun deleteToken(tokenId: String): Int = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("tokens")
            .addEncodedPathSegment(tokenId)
            .build()

        val request: Request = Request.Builder().url(url).delete().build()

        runCatching { okHttpProvider.client.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching { response.code }.fold(
                        onSuccess = { dto -> it.resume(dto) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun deleteAccount(): Int = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("accounts")
            .addPathSegment("me")
            .addPathSegment("initiate_deletion")
            .build()

        val request: Request = Request.Builder().url(url)
            .post(EMPTY_REQUEST).build()

        runCatching { okHttpProvider.client.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching { response.code }.fold(
                        onSuccess = { dto -> it.resume(dto) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun deleteNotification(id: String): Unit = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("app_notifications")
            .addEncodedPathSegment(id)
            .build()

        val request: Request = Request.Builder().url(url).delete().build()

        runCatching { okHttpProvider.client.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching {
                        response.body?.string().toString()
                    }.fold(
                        onSuccess = { _ -> it.resume(Unit) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun listenEvents(lastEventId: String?): Flow<CommerceSessionEvent> =
        callbackFlow {
            val url = HttpUrl.Builder()
                .scheme(SCHEME).host(host)
                .addPathSegment("events")
                .addEncodedQueryParameter(
                    "type",
                    "commerce_session.created," +
                            "commerce_session.completed," +
                            "commerce_session.updated"
                )
                .build()
            val sseClient = okHttpProvider.sseClient
            val sseRequest = Request.Builder()
                .apply {
                    lastEventId?.let {
                        addHeader("Last-Event-ID", it)
                    }
                }
                .url(url)
                .build()

            val sseEventSourceListener = object : EventSourceListener() {
                override fun onClosed(eventSource: EventSource) {
                    Log.d("TAG", "onClosed: $eventSource")
                    close()
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    Log.d("TAG", "onEvent: $data eventSource: $eventSource id: $id, type: $type")
                    when (type) {
                        "commerce_session.created" -> {
                            val dto = json.decodeFromString<CommerceSession>(data)
                            trySend(CommerceSessionEvent.Created(id, dto))
                        }

                        "commerce_session.updated" -> {
                            val dto = json.decodeFromString<CommerceSession>(data)
                            trySend(CommerceSessionEvent.Updated(id, dto))
                        }

                        "commerce_session.completed" -> {
                            val dto = json.decodeFromString<CommerceSession>(data)
                            trySend(CommerceSessionEvent.Completed(id, dto))
                        }

                        else -> {}
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    Log.e("TAG", "onFailure: ", t)
                    cancel(message = response?.message ?: "", cause = t)
                }

                override fun onOpen(eventSource: EventSource, response: Response) {
                    Log.d("TAG", "onOpen: ${response.code} ${response.message}")
                }
            }
            val eventSource = EventSources.createFactory(sseClient)
                .newEventSource(request = sseRequest, listener = sseEventSourceListener)
            awaitClose { eventSource.cancel() }
        }

    override suspend fun getBrands(legacyOnly: Boolean?, startingAfter: String?): BrandsResponse =
        suspendCancellableCoroutine {
            var builder = HttpUrl.Builder().scheme(SCHEME).host(host)
                .addPathSegment("brands")
                .addEncodedQueryParameter("limit", "100")
            when (legacyOnly) {
                null -> {}
                true -> {
                    builder = builder
                        .addEncodedQueryParameter("query", "-legacy_flexcodes:null")
                }

                false -> {
                    builder = builder
                        .addEncodedQueryParameter("query", "legacy_flexcodes:null")
                }
            }
            if (startingAfter != null) {
                builder = builder.addEncodedQueryParameter("starting_after", startingAfter)
            }

            val url = builder.build()

            val request: Request = Request.Builder().url(url).get().build()

            runCatching { okHttpProvider.client.newCall(request).execute() }
                .fold(
                    onSuccess = { response ->
                        runCatching {
                            val raw = response.body?.string().toString()
                            val jsonElement = json.parseToJsonElement(raw)
                            val hasMore = jsonElement.jsonObject["has_more"].toString().toBoolean()
                            val dataObject =
                                jsonElement.jsonObject["data"] ?: JsonObject(emptyMap())
                            val data = json.decodeFromJsonElement<List<Brand>>(dataObject)
                            val lastId = if (hasMore) data.lastOrNull()?.id else null
                            Log.d("TAG", "getBrands loop: hasMore:$hasMore lastId:$lastId")
                            val dto = BrandsResponse(startingAfter = lastId, data = data)
                            dto
                        }.fold(
                            onSuccess = { dto -> it.resume(dto) },
                            onFailure = { ex -> it.resumeWithException(ex) }
                        )
                    },
                    onFailure = { ex -> it.resumeWithException(ex) }
                )

        }

    override suspend fun createCommerceSession(
        brandId: String,
        amount: String,
        assetId: String,
        paymentAssetId: String
    ): CommerceSession.Data = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("commerce_sessions")
            .build()

        val body = buildJsonObject {
            put("brand", brandId)
            put("amount", amount)
            put("asset", assetId)
            val preferences = buildJsonObject { put("payment_asset", paymentAssetId) }
            put("preferences", preferences)
        }.run { toString().toRequestBody(mediaType) }

        val request: Request = Request.Builder().url(url)
            .post(body).build()

        runCatching { okHttpProvider.client.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching {
                        if (response.code != 201) {
                            throw NullPointerException()
                        } else {
                            val raw = response.body?.string().toString()
                            val dto = json.decodeFromString<CommerceSession.Data>(raw)
                            dto
                        }
                    }.fold(
                        onSuccess = { dto -> it.resume(dto) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun closeCommerceSession(commerceSessionId: String): String =
        suspendCancellableCoroutine {
            val url = HttpUrl.Builder()
                .scheme(SCHEME).host(host)
                .addPathSegment("commerce_sessions")
                .addEncodedPathSegment(commerceSessionId)
                .addPathSegment("close")
                .build()

            val request: Request = Request.Builder().url(url)
                .post(EMPTY_REQUEST).build()

            runCatching { okHttpProvider.client.newCall(request).execute() }
                .fold(
                    onSuccess = { response ->
                        runCatching {
                            val raw = response.body?.string().toString()
                            raw
                        }.fold(
                            onSuccess = { dto -> it.resume(dto) },
                            onFailure = { ex -> it.resumeWithException(ex) }
                        )
                    },
                    onFailure = { ex -> it.resumeWithException(ex) }
                )
        }

    override suspend fun confirmTransaction(
        commerceSessionId: String,
        txSignature: String
    ): String = suspendCancellableCoroutine {
        val url = HttpUrl.Builder()
            .scheme(SCHEME).host(host)
            .addPathSegment("transactions")
            .addEncodedPathSegment(commerceSessionId)
            .build()

        val body = buildJsonObject {
            put("signature", txSignature)
        }.run { toString().toRequestBody(mediaType) }

        val request: Request = Request.Builder().url(url)
            .patch(body).build()

        runCatching { okHttpProvider.client.newCall(request).execute() }
            .fold(
                onSuccess = { response ->
                    runCatching {
                        val raw = response.body?.string().toString()
                        raw
                    }.fold(
                        onSuccess = { res -> it.resume(res) },
                        onFailure = { ex -> it.resumeWithException(ex) }
                    )
                },
                onFailure = { ex -> it.resumeWithException(ex) }
            )
    }

    override suspend fun patchCommerceSession(
        commerceSessionId: String,
        paymentAssetId: String
    ): String =
        suspendCancellableCoroutine {
            val url = HttpUrl.Builder()
                .scheme(SCHEME).host(host)
                .addPathSegment("commerce_sessions")
                .addEncodedPathSegment(commerceSessionId)
                .build()

            val body = buildJsonObject {
                val preferences = buildJsonObject {
                    put("payment_asset", paymentAssetId)
                }
                put("preferences", preferences)
            }.run { toString().toRequestBody(mediaType) }

            val request: Request = Request.Builder().url(url)
                .patch(body).build()

            runCatching { okHttpProvider.client.newCall(request).execute() }
                .fold(
                    onSuccess = { response ->
                        runCatching {
                            val raw = response.body?.string().toString()
                            raw
                        }.fold(
                            onSuccess = { res -> it.resume(res) },
                            onFailure = { ex -> it.resumeWithException(ex) }
                        )
                    },
                    onFailure = { ex -> it.resumeWithException(ex) }
                )
        }

    override suspend fun getCommerceSession(sessionId: String): CommerceSession.Data =
        suspendCancellableCoroutine {
            val url = HttpUrl.Builder()
                .scheme(SCHEME).host(host)
                .addPathSegment("commerce_sessions")
                .addEncodedPathSegment(sessionId)
                .build()

            val request: Request = Request.Builder().url(url).get().build()

            runCatching { okHttpProvider.client.newCall(request).execute() }
                .fold(
                    onSuccess = { response ->
                        runCatching {
                            val res = response.body?.string().toString()
                            if (response.code != 200) {
                                Result.failure(NullPointerException())
                            } else {
                                val dto = json.decodeFromString<CommerceSession.Data>(res)
                                Result.success(dto)
                            }
                        }.fold(
                            onSuccess = { res ->
                                if (res.isSuccess) {
                                    res.getOrNull()?.let { r -> it.resume(r) }
                                } else {
                                    res.exceptionOrNull()?.let { e ->
                                        it.resumeWithException(e)
                                    }
                                }
                            },
                            onFailure = { ex -> it.resumeWithException(ex) }
                        )
                    },
                    onFailure = { ex -> it.resumeWithException(ex) }
                )
        }

    override suspend fun getQuote(assetId: String, amount: String, unitOfAccount: String): Quote =
        suspendCancellableCoroutine {
            val url = HttpUrl.Builder()
                .scheme(SCHEME).host(host)
                .addPathSegment("asset_converter")
                .build()

            val body = buildJsonObject {
                put("amount", amount)
                put("asset", assetId)
                put("unit_of_account", unitOfAccount)
            }.run { toString().toRequestBody(mediaType) }

            val cacheControl = CacheControl.Builder()
                .maxAge(25, TimeUnit.SECONDS)
                .build()

            val request: Request = Request.Builder().url(url)
                .cacheControl(cacheControl)
                .put(body).build()

            runCatching { okHttpProvider.client.newCall(request).execute() }
                .fold(
                    onSuccess = { response ->
                        runCatching {
                            val res = response.body?.string().toString()
                            if (response.code != 200) {
                                Result.failure(NullPointerException())
                            } else {
                                val dto = json.decodeFromString<Quote>(res)
                                Result.success(dto)
                            }
                        }.fold(
                            onSuccess = { res ->
                                if (res.isSuccess) {
                                    res.getOrNull()?.let { r -> it.resume(r) }
                                } else {
                                    res.exceptionOrNull()?.let { e ->
                                        it.resumeWithException(e)
                                    }
                                }
                            },
                            onFailure = { ex -> it.resumeWithException(ex) }
                        )
                    },
                    onFailure = { ex -> it.resumeWithException(ex) }
                )
        }
}
