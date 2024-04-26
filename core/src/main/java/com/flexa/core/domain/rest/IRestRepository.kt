package com.flexa.core.domain.rest

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
import com.flexa.core.shared.BrandsResponse
import com.flexa.identity.create_id.AccountsRequest
import kotlinx.coroutines.flow.Flow

interface IRestRepository {

    suspend fun tokens(email: String, challenge: String): TokensResponse
    suspend fun accounts(request: AccountsRequest): Int
    suspend fun patchTokens(
        id: String, verifier: String, challenge: String,
        code: String? = null, link: String? = null
    ): TokenPatch

    suspend fun putAccounts(accounts: List<AppAccount>): PutAppAccountsResponse
    suspend fun getAssets(pageSize: Int, startingAfter: String?): AssetsResponse
    suspend fun getAssetById(assetId: String): Asset
    suspend fun getAccount(): Account
    suspend fun deleteToken(tokenId: String): Int
    suspend fun deleteAccount(): Int
    suspend fun deleteNotification(id: String)
    suspend fun listenEvents(): Flow<CommerceSessionEvent>
    suspend fun getBrands(legacyOnly: Boolean?, startingAfter: String?): BrandsResponse
    suspend fun createCommerceSession(
        brandId: String, amount: String, assetId: String, paymentAssetId: String
    ): CommerceSession

    suspend fun closeCommerceSession(commerceSessionId: String): String
    suspend fun confirmTransaction(
        commerceSessionId: String, txSignature: String
    ): String
    suspend fun patchCommerceSession(
        commerceSessionId: String, paymentAssetId: String
    ): String

    suspend fun getQuote(assetId: String, amount: String, unitOfAccount: String): Quote
}
