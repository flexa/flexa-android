package com.flexa.core.domain.rest

import com.flexa.core.entity.Account
import com.flexa.core.entity.CommerceSessionEvent
import com.flexa.core.entity.PutAppAccountsResponse
import com.flexa.core.entity.TokenPatch
import com.flexa.core.entity.TokensResponse
import com.flexa.core.shared.AppAccount
import com.flexa.core.shared.Asset
import com.flexa.core.shared.AssetsResponse
import com.flexa.identity.create_id.AccountsRequest
import kotlinx.coroutines.flow.Flow

class RestInteractor(
    private val repository: IRestRepository,
) {
    suspend fun tokens(email: String, challenge: String): TokensResponse =
        repository.tokens(email, challenge)

    suspend fun accounts(request: AccountsRequest): Int =
        repository.accounts(request)

    suspend fun tokenPatch(
        id: String, verifier: String, challenge: String, code: String?, link: String?
    ): TokenPatch =
        repository.patchTokens(
            id = id, verifier = verifier, challenge = challenge,
            code = code, link = link
        )

    suspend fun putAccounts(accounts: List<AppAccount>): PutAppAccountsResponse {
        return repository.putAccounts(accounts)
    }

    suspend fun getAssets(pageSize: Int, startingAfter: String?): AssetsResponse =
        repository.getAssets(pageSize, startingAfter)

    suspend fun getAssetById(assetId: String): Asset =
        repository.getAssetById(assetId)

    suspend fun getAccount(): Account = repository.getAccount()

    suspend fun deleteToken(tokenId: String): Int = repository.deleteToken(tokenId)

    suspend fun deleteAccount(): Int = repository.deleteAccount()

    suspend fun deleteNotification(id: String): Unit = repository.deleteNotification(id)

    suspend fun listenEvents(): Flow<CommerceSessionEvent> =
        repository.listenEvents()

    suspend fun getBrands(legacyOnly: Boolean?, startingAfter: String?) =
        repository.getBrands(legacyOnly, startingAfter)

    suspend fun createCommerceSession(
        brandId: String,
        amount: String,
        assetId: String,
        paymentAssetId: String
    ) =
        repository.createCommerceSession(brandId, amount, assetId, paymentAssetId)

    suspend fun closeCommerceSession(commerceSessionId: String) =
        repository.closeCommerceSession(commerceSessionId)

    suspend fun confirmTransaction(
        commerceSessionId: String, txSignature: String
    ) =
        repository.confirmTransaction(commerceSessionId, txSignature)

    suspend fun patchCommerceSession(
        commerceSessionId: String, paymentAssetId: String
    ) = repository.patchCommerceSession(commerceSessionId, paymentAssetId)

    suspend fun getQuote(assetId: String, amount: String, unitOfAccount: String) =
        repository.getQuote(assetId, amount, unitOfAccount)
}
