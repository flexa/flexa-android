package com.flexa.core.entity

import com.flexa.core.shared.Asset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

class PutAppAccountsResponse(
    val hasMore: Boolean,
    val date: String,
    val accounts: List<AppAccount>
)


@Serializable
data class AppAccount(
    @SerialName("account_id")
    val accountId: String,
    @Transient
    var displayName: String? = null,
    @Transient
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
    @SerialName("label")
    val label: String? = null,
    @SerialName("value")
    val value: AssetValue? = null,
    @SerialName("livemode")
    val livemode: Boolean? = null,
    @SerialName("assetData")
    val assetData: Asset? = null,
    @Transient
    val icon: String? = null
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
