package com.flexa.core.entity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionFee(
    @SerialName("asset")
    val asset: String,
    @SerialName("amount")
    val amount: String? = null,
    @SerialName("equivalent")
    val equivalent: String? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("price")
    val price: TransactionFeePrice? = null,
    @SerialName("unit_of_account")
    val unitOfAccount: String? = null,
    @SerialName("zone")
    val zone: String? = null
)
@Serializable
data class TransactionFeePrice(
    @SerialName("amount")
    val amount: String? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("priority")
    val priority: String? = null
)
