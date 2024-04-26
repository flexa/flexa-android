package com.flexa.spend

import android.annotation.SuppressLint
import android.app.Application
import com.flexa.core.Flexa
import com.flexa.core.shared.FlexaConstants
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.shared.SerializerProvider
import com.flexa.core.shared.observeConnectionAsFlow
import com.flexa.spend.data.SecuredPreferences
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.domain.SpendInteractor
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

internal object Spend {
    val selectedAsset: StateFlow<SelectedAsset?> = Flexa.selectedAsset
    var onTransactionRequest: ((Result<Transaction>) -> Unit)? = null
    internal var transactionSent: ((
        @ParameterName("commerceSessionId") String,
        @ParameterName("txSignature") String
    ) -> Unit)? = null
    internal val json = Json { ignoreUnknownKeys = true }

    private val securedPreferences by lazy {
        SecuredPreferences(
            Flexa.context as Application,
            SerializerProvider(), FlexaConstants.FILE
        )
    }
    private var USE_FAKE_INTERACTOR = false

    @SuppressLint("StaticFieldLeak")
    val interactor = if (USE_FAKE_INTERACTOR) FakeInteractor()
    else SpendInteractor(
        Flexa.restInteractor, Flexa.dbInteractor,
        securedPreferences, Flexa.context?.observeConnectionAsFlow()
    )

    fun selectedAsset(value: SelectedAsset) {
        Flexa.selectedAsset(value.accountId, value.asset.assetId)
    }
}

data class Transaction(
    val commerceSessionId: String,
    val amount: String,
    val brandLogo: String,
    val brandName: String,
    val brandColor: String,
    val appAccountId: String,
    val assetId: String,
    val destinationAddress: String,
    val feeAmount: String,
    val feeAssetId: String,
    val feePrice: String,
    val feePriorityPrice: String,
    val size: String,
)
