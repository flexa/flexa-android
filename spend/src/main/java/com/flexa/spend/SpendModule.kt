package com.flexa.spend

import android.app.Activity
import android.content.Intent
import com.flexa.core.Flexa

class SpendConfig private constructor() {

    fun open(activity: Activity, deepLink: String? = null) {
        val intent = Intent(activity, SpendActivity::class.java)
        intent.putExtra(SpendActivity.KEY_DEEP_LINK, deepLink)
        activity.startActivity(intent)
    }

    class Builder {

        fun onTransactionRequest(callback: (Result<Transaction>) -> Unit) = apply {
            Spend.onTransactionRequest = callback
        }

        fun transactionSent(commerceSessionId: String, txSignature: String) {
            Spend.transactionSent?.invoke(commerceSessionId, txSignature)
        }

        fun build() = SpendConfig()

        fun open(activity: Activity, deepLink: String? = null) =
            SpendConfig().open(activity, deepLink)
    }
}

fun Flexa.buildSpend() = SpendConfig.Builder()
