package com.flexa.core.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
class Payment(
    val paymentId: String,
    val merchantId: String,
    val baseAssetId: String,
    val baseAmount: String,
    val quoteAssetId: String,
    val quoteAmount: String,
) : Parcelable
