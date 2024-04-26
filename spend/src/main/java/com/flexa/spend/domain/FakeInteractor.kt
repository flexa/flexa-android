package com.flexa.spend.domain

import com.flexa.core.entity.Account
import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.CommerceSessionEvent
import com.flexa.core.entity.Limit
import com.flexa.core.entity.PutAppAccountsResponse
import com.flexa.core.entity.Quote
import com.flexa.core.shared.Asset
import com.flexa.core.shared.AssetsResponse
import com.flexa.core.shared.Brand
import com.flexa.core.shared.ConnectionState
import com.flexa.spend.MockFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlin.random.Random


internal class FakeInteractor : ISpendInteractor {
    override suspend fun getConnectionListener(): Flow<ConnectionState>? {
        return MutableStateFlow(ConnectionState.Available)
    }

    override suspend fun getLocalAppAccounts(): List<AppAccount> {
        return MockFactory.getMockConfig()
    }

    override suspend fun getAllAssets(pageSize: Int): List<Asset> {
        return listOf(
            Asset(
                id = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp/slip44:501", displayName = "SOL"
            ),
            Asset(
                id = "eip155:1/slip44:60", displayName = "Ether"
            ),
        )
    }

    override suspend fun backupAssetWithKey(asset: AvailableAsset) {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetWithKey(livemode: Boolean): AvailableAsset? {
        TODO("Not yet implemented")
    }

    override suspend fun getAssets(pageSize: Int, nextPageToken: String?): AssetsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getAssetById(assetId: String): Asset {
        TODO("Not yet implemented")
    }

    override suspend fun putAccounts(account: List<com.flexa.core.shared.AppAccount>): PutAppAccountsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getAccount(): Account {
        val remaining = "215.51"
        return Account(
            name = "Satoshi Nakamoto",
            created = 1683146197,
            limits = listOf(
                Limit(
                    amount = "750",
                    asset = "iso4217/USD",
                    description = "\$$remaining remaining",
                    label = "\$750/week",
                    name = "Spend Limit",
                    remaining = remaining,
                    resetsAt = 1723824000
                )
            )
        )
    }

    override suspend fun deleteNotification(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getEmail(): String? {
        return "satoshi.n@gmail.com"
    }

    override suspend fun getPlacesToPayTheme(): String {
        return """
            {
            "android": {
            "light": {
            "backgroundColor": "rgba(255, 255, 255, 1.0)",
            "primary": "#6974ff",
            "cardColor": "azure"
        },
            "dark": {
            "backgroundColor": "rgba(255, 255, 255, 1.0)",
            "primary": "#6974ff",
            "cardColor": "azure"
        }
        }
        }
        """
    }

    override suspend fun getDbAssets(): List<Asset> {
        return listOf(
            Asset(
                id = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp/slip44:501", displayName = "SOL"
            ),
        )
    }

    override suspend fun getDbAssetsById(vararg ids: String): List<Asset> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAssets() {
        TODO("Not yet implemented")
    }

    override suspend fun saveAssets(items: List<Asset>) {
        TODO("Not yet implemented")
    }

    override suspend fun listenEvents(): Flow<CommerceSessionEvent> {
        TODO("Not yet implemented")
    }

    override suspend fun savePinnedBrands(itemsIds: List<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun getPinnedBrands(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getBrands(legacyOnly: Boolean): List<Brand> {
        return listOf(MockFactory.getMockBrand())
    }

    override suspend fun getDbBrands(): List<Brand> {
        return arrayListOf<Brand>().apply {
            repeat(5) {
                Brand(
                    id = "",
                    "",
                    "",
                    "",
                    null,
                    "https://flexa.network/static/4bbb1733b3ef41240ca0f0675502c4f7/d8419/flexa-logo%403x.png",
                    "flexa",
                    "",
                    ""
                )
            }
        }
    }

    override suspend fun saveBrands(items: List<Brand>) {
    }

    override suspend fun deleteBrands() {
    }

    override suspend fun createCommerceSession(
        brandId: String,
        amount: String,
        assetId: String,
        paymentAssetId: String
    ): CommerceSession {
        TODO("Not yet implemented")
    }

    override suspend fun closeCommerceSession(commerceSessionId: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun confirmTransaction(
        commerceSessionId: String,
        txSignature: String
    ): String {
        TODO("Not yet implemented")
    }

    override suspend fun patchCommerceSession(
        commerceSessionId: String,
        txSignature: String
    ): String {
        TODO("Not yet implemented")
    }

    override suspend fun getQuote(
        assetId: String, amount: String, unitOfAccount: String
    ): Quote = withContext(Dispatchers.IO) {
        delay(Random.nextLong(300, 2000))
        MockFactory.getMockQuote()
    }

    override suspend fun deleteToken(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(): Int {
        TODO("Not yet implemented")
    }
}
