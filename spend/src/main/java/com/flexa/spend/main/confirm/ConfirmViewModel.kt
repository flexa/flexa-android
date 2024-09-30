package com.flexa.spend.main.confirm

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.entity.CommerceSession
import com.flexa.core.shared.ApiErrorHandler
import com.flexa.core.shared.SelectedAsset
import com.flexa.spend.Spend
import com.flexa.spend.Transaction
import com.flexa.spend.domain.ISpendInteractor
import com.flexa.spend.toTransaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class ConfirmViewModel(
    internal val session: StateFlow<CommerceSession?>,
    private val interactor: ISpendInteractor = Spend.interactor
) : ViewModel() {

    var payProgress by mutableStateOf(false)
    val buttonsBlocked by derivedStateOf { payProgress || patchProgress }
    var completed by mutableStateOf(false)
    val errorHandler = ApiErrorHandler()
    val transaction = MutableStateFlow<Transaction?>(null)
    var patchProgress by mutableStateOf(false)
    private val _showBalanceRestrictions = MutableStateFlow<Boolean>(false)
    val showBalanceRestrictions = _showBalanceRestrictions.asStateFlow()
    private var watchDefaultAssetJob: Job? = null
    private var intentEventId: String = ""

    init {
        listenCommerceSession()
        listenSelectedAsset()
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }

    fun clear() {
        watchDefaultAssetJob?.cancel()
        transaction.value = null
        intentEventId = ""
        completed = false
    }

    fun showBalanceRestrictions(show: Boolean) {
        _showBalanceRestrictions.value = show
    }

    fun payNow() {
        viewModelScope.launch {
            session.value?.let { commerceSession ->
                payProgress = true
                delay(1000)
                val t = commerceSession.toTransaction()
                payProgress = false
                completed = true
                delay(2000)
                transaction.value = t
            }
        }
    }

    private fun listenCommerceSession() {
        viewModelScope.launch {
            session.collect {
                patchProgress = false
            }
        }
    }

    private fun listenSelectedAsset() {
        viewModelScope.launch {
            Spend.selectedAsset
                .drop(1)
                .collect {
                it?.let { asset ->
                    session.value?.data?.id?.let { sessionId ->
                        Log.d(
                            null,
                            "patchCommerceSession: session:${session.value?.id} asset:${asset.asset.assetId}"
                        )
                        patchCommerceSession(sessionId, asset)
                    }
                }
            }
        }
    }

    private fun patchCommerceSession(commerceSessionId: String, selectedAsset: SelectedAsset) {
        patchProgress = true
        viewModelScope.launch {
            runCatching {
                interactor.patchCommerceSession(
                    commerceSessionId, selectedAsset.asset.assetId
                )
            }.onFailure { patchProgress = false }
                .onSuccess { patchProgress = false }
        }
    }
}
