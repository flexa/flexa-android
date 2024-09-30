package com.flexa.spend.main.keypad

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.entity.ExchangeRate
import com.flexa.core.entity.Quote
import com.flexa.core.shared.SelectedAsset
import com.flexa.spend.domain.ISpendInteractor
import com.flexa.spend.main.main_screen.Event
import com.flexa.spend.main.main_screen.SpendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

internal class AmountDetailViewModel(
    val interactor: ISpendInteractor,
) : ViewModel() {

    var assetAmount: AssetAndAmount? = null
    val quote = MutableStateFlow<Quote?>(null)
    val progress = MutableStateFlow(false)
    val error = MutableStateFlow(false)
    val exchangeRates = MutableStateFlow<List<ExchangeRate>>(emptyList())
    private val percent = MutableStateFlow(0F)

    var percentJob: Job? = null
    private var now: Instant = Instant.now()

    init {
        listenConnection()
        subscribeAppAccounts()
    }

    override fun onCleared() {
        percentJob?.cancel()
        super.onCleared()
    }

    private var subscribeAppAccountsJob: Job? = null
    private fun subscribeAppAccounts() {
        if (subscribeAppAccountsJob?.isActive == true) return
        subscribeAppAccountsJob = viewModelScope.launch {
            kotlin.runCatching { interactor.getDbExchangeRates() }
                .onSuccess { exchangeRates.value = it }
            SpendViewModel.eventFlow.collect { event ->
                if (event is Event.ExchangeRatesUpdate) {
                    exchangeRates.value = event.rates
                }
            }
        }
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
        assetAmount?.let {
            getQuote(
                assetId = it.asset.asset.value?.asset ?: "",
                amount = it.amount,
                unitOfAccount = it.asset.asset.assetId
            )
        }
    }

    private fun getQuote(assetId: String, amount: String, unitOfAccount: String) {
        Log.d(null, "${hashCode()} getQuote: amount: $amount")
        viewModelScope.launch {
            runCatching {
                error.value = false
                progress.value = true
                val res = interactor.getQuote(assetId, amount, unitOfAccount)
                res
            }.onFailure {
                progress.value = false
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

internal class AssetAndAmount(
    val asset: SelectedAsset,
    val amount: String
)