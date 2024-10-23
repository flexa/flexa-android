package com.flexa.core.shared

import android.graphics.Color

data class AssetAccount(
    /**
     * SHA256 hash of Account ID string
     */
    val assetAccountHash: String,
    val custodyModel: CustodyModel,
    val displayName: String? = null,
    val icon: String? = null,
    val availableAssets: List<AvailableAsset>,
)

data class AvailableAsset(
    val assetId: String,
    val balance: Double,
    /**
     * Due to the nature of the UTXO model used on Bitcoin and Zcash,
     * spendable balance can fluctuate, sometimes to zero temporarily
     * as the transaction is being mined and confirmed on-chain
     */
    val balanceAvailable: Double? = null,
    val icon: String? = null,
    val displayName: String? = null,
    val symbol: String? = null,
    val accentColor: Color? = null,
)

enum class CustodyModel {
    LOCAL, MANAGED
}

fun List<AssetAccount>.filterAssets(assets: List<Asset>): List<AssetAccount> {
    val accounts = this.distinctBy { it.assetAccountHash }
    val appAccounts = ArrayList<AssetAccount>(accounts.size)
    accounts.forEach { acc ->
        val filteredAssets = acc.filterAssets(assets)
        appAccounts.add(acc.copy(availableAssets = filteredAssets))
    }
    return appAccounts
}

fun AssetAccount.filterAssets(assets: List<Asset>): List<AvailableAsset> {
    return this.availableAssets.filter { asset ->
        assets.any { it.id == asset.assetId }
    }
}