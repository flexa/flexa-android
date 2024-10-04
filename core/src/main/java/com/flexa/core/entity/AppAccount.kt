package com.flexa.core.entity

import com.flexa.core.shared.Asset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.math.BigDecimal

class PutAppAccountsResponse(
    val date: String,
    val accounts: List<AppAccount>
)

@Serializable
data class AppAccount(
    @SerialName("account_id")
    val accountId: String,
    @SerialName("displayName")
    val displayName: String? = null,
    @SerialName("unitOfAccount")
    val unitOfAccount: String? = null,
    @SerialName("icon")
    val icon: String? = null,
    @SerialName("assets")
    val availableAssets: ArrayList<AvailableAsset> = arrayListOf(),
)

@Serializable
data class AvailableAsset(
    @SerialName("asset")
    val assetId: String,
    @SerialName("balance")
    val balance: String,
    @SerialName("key")
    val key: AssetKey? = null,
    @SerialName("value")
    val value: AssetValue? = null,
    @SerialName("livemode")
    val livemode: Boolean? = null,
    @SerialName("icon")
    val icon: String? = null,
    @SerialName("assetData")
    val assetData: Asset? = null,
    @SerialName("balanceAvailable")
    val balanceAvailable: Double? = null,
    @SerialName("exchangeRate")
    val exchangeRate: ExchangeRate? = null,
    @SerialName("oneTimeKey")
    val oneTimeKey: OneTimeKey? = null,
    @SerialName("fee")
    val fee: TransactionFee? = null,
    @Transient
    val balanceBundle: BalanceBundle? = null,
)

data class BalanceBundle (
    val total: BigDecimal,
    val totalLabel: String,
    val available: BigDecimal? = null,
    val availableLabel: String? = null,
)

@Serializable
data class AssetKey(
    @SerialName("prefix")
    val prefix: String,
    @SerialName("secret")
    val secret: String,
    @SerialName("length")
    val length: Int,
)

@Serializable
data class AssetValue(
    @SerialName("asset")
    val asset: String,
    @SerialName("label")
    val label: String,
    @SerialName("label_titlecase")
    val labelTitlecase: String,
)
