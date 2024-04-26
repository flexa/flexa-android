package com.flexa.scan

import android.app.Activity
import android.content.Intent
import com.flexa.core.Flexa

internal object Scan {
    internal var qrCodesCallback: ((Result<List<String>>) -> Unit)? = null
}

class ScanConfig private constructor() {

    fun open(activity: Activity) {
        activity.startActivity(Intent(activity, ScanActivity::class.java))
    }

    class Builder {

        /**
         * Returns recognized qr codes
         */
        fun onSendCallback(onCodeScanned: (Result<List<String>>) -> Unit) = apply {
            Scan.qrCodesCallback = onCodeScanned
        }

        fun build() = ScanConfig()

        fun open(activity: Activity) = ScanConfig().open(activity)
    }
}

fun Flexa.buildScan() = ScanConfig.Builder()
