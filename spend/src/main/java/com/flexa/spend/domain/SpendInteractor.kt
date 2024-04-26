package com.flexa.spend.domain

import com.flexa.core.domain.db.DbInteractor
import com.flexa.core.domain.rest.RestInteractor
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
import com.flexa.core.shared.FlexaConstants
import com.flexa.spend.Spend.json
import com.flexa.spend.data.SecuredPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val RETRY_DELAY = 500L
const val RETRY_COUNT = 3

internal class SpendInteractor(
    private val interactor: RestInteractor,
    private val dbInteractor: DbInteractor,
    private val preferences: SecuredPreferences,
    private val connectionListener: Flow<ConnectionState>? = null
) : ISpendInteractor {

    override suspend fun getConnectionListener(): Flow<ConnectionState>? = connectionListener

    override suspend fun getLocalAppAccounts(): List<AppAccount> = withContext(Dispatchers.IO) {
        val data = preferences.getString(FlexaConstants.APP_ACCOUNTS)
        val accounts = data?.let { Json.decodeFromString<List<AppAccount>>(it) }
        accounts ?: emptyList()
    }

    override suspend fun putAccounts(
        account: List<com.flexa.core.shared.AppAccount>
    ): PutAppAccountsResponse = withContext(Dispatchers.IO) {
        val response = interactor.putAccounts(account)

        val allAssets = response.accounts.flatMap { it.availableAssets }
        val livemodeAsset = allAssets.firstOrNull {
            it.livemode == true && it.key != null
        }
        val testmodeAsset = allAssets.firstOrNull {
            it.livemode == false && it.key != null
        }
        livemodeAsset?.let { asset -> backupAssetWithKey(asset) }
        testmodeAsset?.let { asset -> backupAssetWithKey(asset) }

        val accounts = json.encodeToString(response.accounts)
        preferences.saveString(FlexaConstants.APP_ACCOUNTS, accounts)
        response
    }

    override suspend fun getAllAssets(pageSize: Int): List<Asset> =
        withContext(Dispatchers.IO) {
            var startingAfter: String? = null
            val assets = arrayListOf<Asset>()
            do {
                val response = getAssets(pageSize, startingAfter)
                startingAfter = response.startingAfter
                assets.addAll(response.data)
            } while (startingAfter != null)
            assets
        }

    override suspend fun backupAssetWithKey(asset: AvailableAsset) =
        withContext(Dispatchers.IO) {
            val data = json.encodeToString(asset)
            val key = if (asset.livemode == true) FlexaConstants.ASSET_KEY else
                FlexaConstants.ASSET_TESTMODE_KEY
            preferences.saveString(key, data)
        }

    override suspend fun getAssetWithKey(livemode: Boolean): AvailableAsset? =
        withContext(Dispatchers.IO) {
            val key = if (livemode) FlexaConstants.ASSET_KEY else
                FlexaConstants.ASSET_TESTMODE_KEY
            val data = preferences.getString(key)
            if (data?.isNotEmpty() == true) {
                json.decodeFromString<AvailableAsset>(data)
            } else null
        }

    override suspend fun getAssets(pageSize: Int, nextPageToken: String?): AssetsResponse =
        interactor.getAssets(pageSize, nextPageToken)

    override suspend fun getAssetById(assetId: String): Asset = interactor.getAssetById(assetId)

    override suspend fun getAccount() = withContext(Dispatchers.IO) {
        interactor.getAccount()
    }

    override suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        interactor.deleteNotification(id)
    }

    override suspend fun getEmail(): String? {
        return preferences.getString(FlexaConstants.EMAIL)
    }

    override suspend fun getPlacesToPayTheme(): String? {
        return preferences.getString(FlexaConstants.PLACES_TO_PAY_THEME)
    }

    override suspend fun getDbAssets(): List<Asset> =
        withContext(Dispatchers.IO) { dbInteractor.getAssets() }

    override suspend fun getDbAssetsById(vararg ids: String): List<Asset> =
        withContext(Dispatchers.IO) { dbInteractor.getAssetsById(*ids) }

    override suspend fun deleteAssets() =
        withContext(Dispatchers.IO) { dbInteractor.deleteAssets() }

    override suspend fun saveAssets(items: List<Asset>) =
        withContext(Dispatchers.IO) { dbInteractor.saveAssets(items) }

    override suspend fun listenEvents(): Flow<CommerceSessionEvent> =
        interactor.listenEvents()

    override suspend fun savePinnedBrands(itemsIds: List<String>) =
        withContext(Dispatchers.IO) {
            val items = json.encodeToString(itemsIds)
            preferences.saveString(FlexaConstants.PINNED_BRANDS, items)
        }

    override suspend fun getPinnedBrands(): List<String> =
        withContext(Dispatchers.IO) {
            val string = preferences.getString(FlexaConstants.PINNED_BRANDS) ?: "[]"
            json.decodeFromString<List<String>>(string)
        }

    override suspend fun getBrands(legacyOnly: Boolean): List<Brand> =
        withContext(Dispatchers.IO) {
            var startingAfter: String? = null
            val brands = ArrayList<Brand>()
            do {
                val response = interactor.getBrands(legacyOnly, startingAfter)
                startingAfter = response.startingAfter
                brands.addAll(response.data)
            } while (startingAfter != null)
            brands
        }

    override suspend fun getDbBrands(): List<Brand> =
        withContext(Dispatchers.IO) { dbInteractor.getBrands() }

    override suspend fun saveBrands(items: List<Brand>) =
        withContext(Dispatchers.IO) { dbInteractor.saveBrands(items) }

    override suspend fun deleteBrands() =
        withContext(Dispatchers.IO) { dbInteractor.deleteBrands() }

    override suspend fun createCommerceSession(
        brandId: String,
        amount: String,
        assetId: String,
        paymentAssetId: String
    ): CommerceSession =
        withContext(Dispatchers.IO) {
            interactor.createCommerceSession(
                brandId,
                amount,
                assetId,
                paymentAssetId
            )
        }

    override suspend fun closeCommerceSession(commerceSessionId: String): String =
        withContext(Dispatchers.IO) {
            interactor.closeCommerceSession(commerceSessionId)
        }

    override suspend fun confirmTransaction(
        commerceSessionId: String,
        txSignature: String
    ): String = withContext(Dispatchers.IO) {
        interactor.confirmTransaction(commerceSessionId, txSignature)
    }

    override suspend fun patchCommerceSession(
        commerceSessionId: String,
        paymentAssetId: String
    ): String = withContext(Dispatchers.IO) {
        interactor.patchCommerceSession(commerceSessionId, paymentAssetId)
    }

    override suspend fun getQuote(
        assetId: String, amount: String, unitOfAccount: String
    ): Quote =
        withContext(Dispatchers.IO) {
            interactor.getQuote(
                assetId = assetId,
                amount = amount,
                unitOfAccount = unitOfAccount
            )
        }

    override suspend fun deleteToken(): Int = withContext(Dispatchers.IO) {
        val tokenId = preferences.getString(FlexaConstants.TOKEN_ID)?:""
        interactor.deleteToken(tokenId)
    }

    override suspend fun deleteAccount(): Int = withContext(Dispatchers.IO) {
        interactor.deleteAccount()
    }
}
