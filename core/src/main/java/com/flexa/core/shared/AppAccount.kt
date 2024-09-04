package com.flexa.core.shared

import android.graphics.Color

data class AppAccount(
    val accountId: String,
    val custodyModel: CustodyModel,
    val displayName: String? = null,
    val icon: String? = null,
    val availableAssets: List<AvailableAsset>,
)

data class AvailableAsset(
    val assetId: String,
    val balance: Double,
    val icon: String? = null,
    val displayName: String? = null,
    val symbol: String? = null,
    val accentColor: Color? = null,
)

enum class CustodyModel {
    LOCAL, MANAGED
}

fun List<AppAccount>.filterAssets(assets: List<Asset>): List<AppAccount> {
    val accounts = this.distinctBy { it.accountId }
    val appAccounts = ArrayList<AppAccount>(accounts.size)
    accounts.forEach { acc->
        val filteredAssets = acc.availableAssets.filter { asset ->
            assets.any { it.id == asset.assetId }
        }
        appAccounts.add(acc.copy(availableAssets = filteredAssets))
    }
    return appAccounts
}