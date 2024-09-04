package com.flexa.spend.domain

import com.flexa.core.data.db.TransactionBundle
import com.flexa.core.entity.Account
import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.CommerceSessionEvent
import com.flexa.core.entity.PutAppAccountsResponse
import com.flexa.core.entity.Quote
import com.flexa.core.shared.Asset
import com.flexa.core.shared.AssetsResponse
import com.flexa.core.shared.Brand
import com.flexa.core.shared.ConnectionState
import kotlinx.coroutines.flow.Flow

interface ISpendInteractor {

    suspend fun getConnectionListener(): Flow<ConnectionState>?
    suspend fun getLocalAppAccounts(): List<AppAccount>
    suspend fun getAllAssets(pageSize: Int = 100): List<Asset>
    suspend fun backupAssetWithKey(asset: AvailableAsset)
    suspend fun getAssetWithKey(livemode: Boolean): AvailableAsset?
    suspend fun getAssets(pageSize: Int, nextPageToken: String? = null): AssetsResponse
    suspend fun getAssetById(assetId: String): Asset
    suspend fun getTransactionBySessionId(sessionId: String): TransactionBundle?
    suspend fun deleteTransaction(sessionId: String)
    suspend fun deleteOutdatedTransactions()
    suspend fun saveTransaction(transactionBundle: TransactionBundle)
    suspend fun putAccounts(account: List<com.flexa.core.shared.AppAccount>): PutAppAccountsResponse
    suspend fun getAccount(): Account
    suspend fun deleteNotification(id: String)
    suspend fun getEmail(): String?
    suspend fun getPublishableKey(): String
    suspend fun getPlacesToPayTheme(): String?
    suspend fun getDbAssets(): List<Asset>
    suspend fun getDbAssetsById(vararg ids: String): List<Asset>
    suspend fun deleteAssets()
    suspend fun saveAssets(items: List<Asset>)
    suspend fun listenEvents(): Flow<CommerceSessionEvent>
    suspend fun savePinnedBrands(itemsIds: List<String>)
    suspend fun getPinnedBrands(): List<String>
    suspend fun getBrands(legacyOnly: Boolean): List<Brand>
    suspend fun getDbBrands(): List<Brand>
    suspend fun saveBrands(items: List<Brand>)
    suspend fun deleteBrands()
    suspend fun createCommerceSession(
        brandId: String, amount: String, assetId: String, paymentAssetId: String
    ): CommerceSession
    suspend fun closeCommerceSession(commerceSessionId: String): String
    suspend fun confirmTransaction(
        commerceSessionId: String, txSignature: String): String
    suspend fun patchCommerceSession(
        commerceSessionId: String, paymentAssetId: String): String
    suspend fun getQuote(assetId: String, amount: String, unitOfAccount: String): Quote
    suspend fun deleteToken(): Int
    suspend fun deleteAccount(): Int
}
