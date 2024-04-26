package com.flexa.spend.main.assets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.entity.Quote
import com.flexa.core.shared.SelectedAsset
import com.flexa.spend.domain.ISpendInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

internal class AssetDetailViewModel(
    val interactor: ISpendInteractor,
) : ViewModel() {

    var asset: SelectedAsset? = null
    val quote = MutableStateFlow<Quote?>(null)
    val progress = MutableStateFlow(false)
    val error = MutableStateFlow(false)
    val percent = MutableStateFlow(0F)

    var percentJob: Job? = null
    private var now: Instant = Instant.now()

    init {
        listenConnection()
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }

    internal fun clear() {
        percentJob?.cancel()
        quote.value = null
        error.value = false
    }

    private fun listenConnection() {
        viewModelScope.launch {
            interactor.getConnectionListener()
                ?.distinctUntilChanged()
                ?.collect {
                    if (error.value) getQuote()
                }
        }
    }

    fun getQuote() {
        percentJob?.cancel()
        asset?.let {
            getQuote(
                assetId = it.asset.assetId,
                amount = it.asset.balance,
                unitOfAccount = it.asset.value?.asset ?: ""
            )
        }
    }

    private fun getQuote(assetId: String, amount: String, unitOfAccount: String) {
        Log.d(null, "${hashCode()} getQuote: amount: $amount")
        viewModelScope.launch {
            runCatching {
                error.value = false
                progress.value = true
                interactor.getQuote(assetId, amount, unitOfAccount)
            }.onFailure {
                progress.value = false
                error.value = true
            }.onSuccess {
                progress.value = false
                quote.value = it
                it.value?.rate?.expiresAt?.let { expiresAt ->
                    calculateElapsedPercent(expiresAt * 1000)
                }
            }
        }
    }

    private fun calculateElapsedPercent(futureTimestamp: Long) {
        percentJob = viewModelScope.launch(Dispatchers.IO) {
            now = Instant.now()
            do {
                val p = calculatePercentFromNow(futureTimestamp)
                percent.value = p.toFloat()
                delay(60)
            } while (isActive && p >= 0F)
            getQuote()
        }
    }

    private fun calculatePercentFromNow(futureTimestamp: Long): Double {
        val totalDuration = Duration.between(now, Instant.ofEpochMilli(futureTimestamp)).toMillis()
        val elapsedDuration = Duration.between(now, Instant.now()).toMillis()
        val elapsedPercentage = (elapsedDuration.toDouble() / totalDuration)
        return 1F - elapsedPercentage
    }
}
