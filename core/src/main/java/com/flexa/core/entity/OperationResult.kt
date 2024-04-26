package com.flexa.core.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

sealed class OperationResult

/**
 * The parameters t, a, c, and s should be parsed, and their values represent:
 *
 * t => tokenId - A unique token that uniquely identifies a physical point of sale where a customer is scanning the qr code to pay.
 *
 * a => amount - The amount of the payment requested in the currencies smallest unit of currency (e.g. cents for USD, so 2499 is $24.99)
 *
 * c => currency code - The currency code of the payment amount
 *
 * s => auditId - A client (in the case our client is a point of sale machine) generated, unique token that will be used by Flexa Payment API to communicate back to the point of sale about the success or the failure of the payment
 */
@Parcelize
data class PaymentEntity(
    val amountCents: String?,
    val posToken: String?,
    val currency: String?,
    val auditId: String?,
    val assetId: String?
) : Parcelable, OperationResult()

@Parcelize
data class PaymentEntityV2(
    val qrCodeData: String
) : Parcelable, OperationResult()


data class PaymentResponse(
    @field:SerializedName("auditId")
    val auditId: String? = null,
    @field:SerializedName("amount")
    val amount: String? = null,
    @field:SerializedName("currency")
    val currency: String? = null,
    @field:SerializedName("status")
    val status: String? = null
) : OperationResult()


@Parcelize
class AuthloadEntity(
    val transactionID: String,
    var currency: String? = null,
    var value: Double? = 0.0
) : Parcelable, OperationResult()


data class OperationResultError(
    val error: Throwable
) : OperationResult()


class OperationResultProgress(
    val progress: Boolean
) : OperationResult()
