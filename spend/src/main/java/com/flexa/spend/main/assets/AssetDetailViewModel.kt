package com.flexa.spend.main.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.entity.ExchangeRate
import com.flexa.core.entity.Quote
import com.flexa.core.shared.SelectedAsset
import com.flexa.spend.domain.ISpendInteractor
import com.flexa.spend.main.main_screen.Event
import com.flexa.spend.main.main_screen.SpendViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class AssetDetailViewModel(
    val interactor: ISpendInteractor,
) : ViewModel() {

    var asset: SelectedAsset? = null
    val quote = MutableStateFlow<Quote?>(null)
    val progress = MutableStateFlow(false)
    val error = MutableStateFlow(false)
    val exchangeRates = MutableStateFlow<List<ExchangeRate>>(emptyList())

    private var percentJob: Job? = null

    init {
        subscribeAppAccounts()
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

    private var subscribeAppAccountsJob: Job? = null
    private fun subscribeAppAccounts() {
        if (subscribeAppAccountsJob?.isActive == true) return

        subscribeAppAccountsJob = viewModelScope.launch {
            kotlin.runCatching { interactor.getDbExchangeRates() }
                .onSuccess { exchangeRates.value = it }
            SpendViewModel.eventFlow.collect { event ->
                if (event is Event.ExchangeRatesUpdate) {
                    exchangeRates.value = event.items
                }
            }
        }
    }
}
