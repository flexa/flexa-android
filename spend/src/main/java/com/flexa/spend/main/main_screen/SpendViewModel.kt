package com.flexa.spend.main.main_screen

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.flexa.core.Flexa
import com.flexa.core.entity.Account
import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.CommerceSessionEvent
import com.flexa.core.entity.Notification
import com.flexa.core.shared.ApiErrorHandler
import com.flexa.core.shared.Brand
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.shared.filterAssets
import com.flexa.core.toDate
import com.flexa.spend.Spend
import com.flexa.spend.containsAuthorization
import com.flexa.spend.domain.CommerceSessionWorker
import com.flexa.spend.domain.ISpendInteractor
import com.flexa.spend.isLegacy
import com.flexa.spend.isValid
import com.flexa.spend.toTransaction
import com.flexa.spend.toTransactionBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class SpendViewModel(
    private val interactor: ISpendInteractor = Spend.interactor,
) : ViewModel() {

    companion object {
        val eventFlow = MutableSharedFlow<Event>()
        var livemodeAsset: AvailableAsset? = null
        var testmodeAsset: AvailableAsset? = null
    }

    val confirmViewModelStore = ViewModelStore()
    val brand = MutableStateFlow<Brand?>(null)
    val amount = MutableStateFlow<String?>(null)
    val duration = MutableStateFlow<Duration>(Duration.ZERO)
    private val _progress = MutableStateFlow(false)
    val progress: StateFlow<Boolean> = _progress
    private val _timeout = MutableStateFlow(false)
    val timeout: StateFlow<Boolean> = _timeout
    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account
    private val _notifications = mutableStateListOf<Notification>()
    val notifications: List<Notification> = _notifications

    private val _commerceSession = MutableStateFlow<CommerceSession?>(null)
    val commerceSession: StateFlow<CommerceSession?> = _commerceSession

    var sheetScreen by mutableStateOf<SheetScreen>(SheetScreen.Assets)
    val errorHandler = ApiErrorHandler()
    var openLegacyCard = MutableStateFlow(false)

    init {
        initCachedAppAccount()
        listenConnection()
        listenCommerceSession()
        listenTransactionSent()
        deleteOutdatedTransactions()
    }

    override fun onCleared() {
        cancelTimeout()
        confirmViewModelStore.clear()
        super.onCleared()
    }

    internal fun stopProgress() {
        _progress.value = false
    }

    internal fun deleteCommerceSessionData() {
        viewModelScope.launch {
            _commerceSession.emit(null)
        }
    }

    internal fun createCommerceSession(
        brandId: String, amount: String,
        assetId: String, paymentAssetId: String
    ) {
        viewModelScope.launch {
            _progress.value = true
            kotlin.runCatching {
                interactor.createCommerceSession(
                    brandId, amount, assetId, paymentAssetId
                )
            }.onFailure {
                stopProgress()
            }
                .onSuccess { session ->
                    if (session.isValid())
                        _commerceSession.emit(session)
                    initCloseSessionTimeout()
                }

        }
    }

    private var timeoutJob: Job? = null
    internal fun initCloseSessionTimeout() {
        timeoutJob = viewModelScope.launch {
            _timeout.value = false
            delay(60_000)
            if (isActive) {
                _timeout.value = true
            }
        }
    }

    internal fun cancelTimeout() {
        _timeout.value = false
        timeoutJob?.cancel()
    }

    internal fun closeCommerceSession(context: Context, sessionId: String) {
        stopProgress()
        CommerceSessionWorker.execute(context, sessionId)
    }

    internal fun removeNotification(notification: Notification) {
        _notifications.remove(notification)
        viewModelScope.launch {
            interactor.deleteNotification(notification.id ?: "")
        }
    }

    internal fun getAccount() {
        viewModelScope.launch {
            runCatching {
                interactor.getAccount()
            }.onFailure { Log.e(null, "getAccount: ", it) }
                .onSuccess { account ->
                    _account.value = account
                    account.notifications?.let { n ->
                        _notifications.clear()
                        _notifications.addAll(n)
                    }
                }
        }
    }

    private fun listenEvents() {
        viewModelScope.launch {
            interactor.listenEvents()
                .flowOn(Dispatchers.IO)
                .retryWhen { _, attempt ->
                    delay(1000)
                    attempt < 100
                }
                .catch { Log.e(null, "listenEvents: ", it) }
                .collect { event ->
                    when (event) {
                        is CommerceSessionEvent.Created -> {
                            if (event.session.isValid())
                                _commerceSession.emit(event.session)
                        }

                        is CommerceSessionEvent.Updated -> {
                            if (event.session.isValid()) {
                                _commerceSession.emit(event.session)
                            }
                        }

                        is CommerceSessionEvent.Canceled -> {
                            _commerceSession.emit(null)
                        }

                        else -> {}
                    }
                }
        }
    }

    private fun listenConnection() {
        viewModelScope.launch {
            interactor.getConnectionListener()
                ?.distinctUntilChanged()
                ?.collect {
                    getAccount()
                    listenFlexaAppAccounts()
                    listenEvents()
                }
        }
    }

    private fun initCachedAppAccount() {
        viewModelScope.launch {
            eventFlow.emit(Event.AppAccountsUpdate(interactor.getLocalAppAccounts()))
        }
    }

    private fun listenFlexaAppAccounts() {
        viewModelScope.launch {
            Flexa.appAccounts.collect {
                Log.d("TAG", "listenFlexaAppAccounts: $it")
                runCatching {
                    val assets = interactor.getAllAssets()
                    interactor.deleteAssets()
                    interactor.saveAssets(assets)
                    val putAccount =
                        interactor.putAccounts(Flexa.appAccounts.value.filterAssets(assets))
                    this@SpendViewModel.duration.value = getDuration(putAccount.date)
                    putAccount.accounts
                }.onSuccess { res ->
                    eventFlow.emit(Event.AppAccountsUpdate(res))
                }
            }
        }
    }

    private var sentSessionId: String? = null
    private fun listenCommerceSession() {
        viewModelScope.launch {
            _commerceSession.collect { cs ->
                val legacy = cs.isLegacy()
                val containsAuthorization = cs.containsAuthorization()
                if (legacy) {
                    cs?.toTransactionBundle()?.let {
                        interactor.saveTransaction(it)
                    }
                }

                when {
                    legacy && containsAuthorization -> {
                        openLegacyCard.value = true
                    }

                    legacy && sentSessionId != cs?.data?.id -> {
                        cs?.toTransaction()?.let { transaction ->
                            sentSessionId = transaction.commerceSessionId
                            Spend.onTransactionRequest?.invoke(Result.success(transaction))
                        }
                    }
                }
            }
        }
    }

    private fun listenTransactionSent() {
        if (Flexa.context != null) {
            Spend.transactionSent = { id, txSignature ->
                viewModelScope.launch {
                    interactor.getTransactionBySessionId(id)?.let { transaction ->
                        runCatching {
                            interactor.confirmTransaction(transaction.transactionId, txSignature)
                        }.onFailure { Log.e(null, "listenTransactionSent: ", it) }
                            .onSuccess {

                            }
                    }
                }
            }
        }
    }

    private fun getDuration(date: String): Duration {
        val serverTime = date.toDate().toInstant()
        val clientTime = Instant.now()
        val duration = Duration.between(serverTime, clientTime)
        Log.d("TAG", "getDuration: server: $serverTime")
        Log.d("TAG", "getDuration: local : $clientTime")
        Log.d("TAG", "getDuration: >${duration.toMillis()}<")
        return duration
    }

    private fun deleteOutdatedTransactions() {
        viewModelScope.launch {
            interactor.deleteOutdatedTransactions()
        }
    }
}

sealed class SheetScreen {
    data object Assets : SheetScreen()
    class AssetDetails(val asset: SelectedAsset) : SheetScreen()
    data object PlacesToPay : SheetScreen()
    data object Locations : SheetScreen()
    class PaymentDetails(val session: SharedFlow<CommerceSession?>) : SheetScreen()
}

sealed class Event {
    class AppAccountsUpdate(val accounts: List<AppAccount>) : Event()
}
