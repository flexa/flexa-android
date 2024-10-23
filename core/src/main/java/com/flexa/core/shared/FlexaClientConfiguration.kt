package com.flexa.core.shared

import android.content.Context
import com.flexa.core.theme.FlexaTheme

/**
 * Flexa init bundle
 * @param context Activity or Application Context
 * @param publishableKey
 * @param assetAccounts user wallets
 * @param theme provide color schemes and dynamic colors
 * @param webViewThemeConfig provide color scheme for Places to Pay
 */
class FlexaClientConfiguration(
    val context: Context,
    val publishableKey: String,
    val assetAccounts: ArrayList<AssetAccount>,
    val theme: FlexaTheme = FlexaTheme(),
    val webViewThemeConfig: String? = null
)
