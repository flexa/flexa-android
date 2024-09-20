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
import com.flexa.core.shared.ConnectionState
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.shared.filterAssets
import com.flexa.core.toDate
import com.flexa.spend.Spend
import com.flexa.spend.containsAuthorization
import com.flexa.spend.domain.CommerceSessionWorker
import com.flexa.spend.domain.ISpendInteractor
import com.flexa.spend.isCompleted
import com.flexa.spend.isValid
import com.flexa.spend.toBrandSession
import com.flexa.spend.toTransaction
import com.flexa.spend.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

internal const val ZERO = "0"
private const val RETRY_COUNT = 5
private const val RETRY_DELAY = 1000L
private const val COMPLETE_SESSION_TIMEOUT = 60_000L

class SpendViewModel(
    private val interactor: ISpendInteractor = Spend.interactor,
    private val selectedAsset: StateFlow<SelectedAsset?> = MutableStateFlow(null),
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

    private var lastSessionError = false

    init {
        initCachedAppAccount()
        listenConnection()
        listenCommerceSession()
        listenTransactionSent()
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
        deleteLastSessionId()
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
            }
                .onFailure { stopProgress() }
                .onSuccess { session ->
                    interactor.deleteOutdatedSessions()
                    if (session.isValid()) {
                        saveBrandSession(session)
                        val cs = session.copy(isLegacy = true)
                        saveLastSessionId(cs.id)
                        _commerceSession.emit(CommerceSession(data = cs))
                    }
                    initCloseSessionTimeout()
                }

        }
    }

    private var timeoutJob: Job? = null
    internal fun initCloseSessionTimeout() {
        timeoutJob = viewModelScope.launch {
            _timeout.value = false
            delay(COMPLETE_SESSION_TIMEOUT)
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
        deleteLastSessionId()
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

    private suspend fun isLegacy(
        commerceSession: CommerceSession.Data?
    ): Boolean {
        val brandSession = interactor.getBrandSession(commerceSession?.id ?: "")
        return brandSession != null
    }

    private fun listenEvents() {
        viewModelScope.launch {
            checkLastSession()
            interactor.listenEvents(interactor.getLastEventId())
                .flowOn(Dispatchers.IO)
                .retryWhen { _, attempt ->
                    delay(RETRY_DELAY)
                    attempt < RETRY_COUNT
                }
                .catch { Log.e(null, "listenEvents: ", it) }
                .onEach { event -> event.eventId?.let { interactor.saveLastEventId(it) } }
                .collect { event ->
                    when (event) {
                        is CommerceSessionEvent.Created -> {
                            if (event.session.isValid()) {
                                val updatedSessionEvent = event.session.copy(
                                    data = event.session.data?.copy(isLegacy = isLegacy(event.session.data))
                                )
                                saveLastSessionId(updatedSessionEvent.data?.id)
                                _commerceSession.emit(updatedSessionEvent)
                            }
                        }

                        is CommerceSessionEvent.Updated -> {
                            if (event.session.isValid()) {
                                val updatedSessionEvent = event.session.copy(
                                    data = event.session.data?.copy(isLegacy = isLegacy(event.session.data))
                                )
                                if (updatedSessionEvent.data?.isLegacy == false && updatedSessionEvent.isCompleted()) {
                                    // Close Next-Gen session card
                                    _commerceSession.emit(null)
                                } else {
                                    _commerceSession.emit(updatedSessionEvent)
                                }
                            }
                        }

                        is CommerceSessionEvent.Completed -> {
                            if (event.session.isValid()) {
                                val updatedSessionEvent = event.session.copy(
                                    data = event.session.data?.copy(isLegacy = isLegacy(event.session.data))
                                )
                                if (updatedSessionEvent.data?.isLegacy == false) {
                                    // Close Next-Gen session card
                                    deleteCommerceSessionData()
                                } else {
                                    _commerceSession.emit(updatedSessionEvent)
                                }
                            }
                        }
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
                    checkLastSessionError()
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
                val legacy = cs?.data?.isLegacy ?: false
                val containsAuthorization = cs.containsAuthorization()
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
                    interactor.getBrandSession(id)?.let { transaction ->
                        interactor.getConnectionListener()
                            ?.filter { it == ConnectionState.Available }
                            ?.map {
                                interactor.confirmTransaction(
                                    transaction.transactionId,
                                    txSignature
                                )
                            }
                            ?.retryWhen { _, attempt ->
                                delay(RETRY_DELAY)
                                attempt < RETRY_COUNT
                            }
                            ?.catch { Log.e(null, "listenTransactionSent: ", it) }
                            ?.collect { }
                    }
                }
            }
        }
    }

    private fun saveLastSessionId(sessionId: String?) {
        viewModelScope.launch { interactor.saveLastSessionId(sessionId) }
    }

    private fun deleteLastSessionId() {
        viewModelScope.launch { interactor.saveLastSessionId(null) }
    }

    private fun checkLastSessionError() {
        if (lastSessionError) {
            viewModelScope.launch { checkLastSession() }
        }
    }

    private suspend fun checkLastSession() {
        flow {
            val id = interactor.getLastSessionId()
            emit(id?.let { interactor.getCommerceSession(it) })
        }
            .retryWhen { _, attempt ->
                delay(RETRY_DELAY)
                attempt < RETRY_COUNT
            }
            .catch { lastSessionError = true }
            .filter { it != null }
            .filter { it.isValid() }
            .onEach {
                val selectedAssetId = selectedAsset.value?.asset?.assetId ?: ""
                if (it.transaction()?.asset != selectedAssetId) {
                    interactor.patchCommerceSession(
                        it?.id ?: "", selectedAssetId
                    )
                }
            }.collect { cs ->
                lastSessionError = false
                val updatedSessionEvent = CommerceSession(
                    data = cs?.copy(isLegacy = isLegacy(cs))
                )
                if (updatedSessionEvent.data?.isLegacy == false && updatedSessionEvent.isCompleted()) {
                    _commerceSession.emit(null)
                } else {
                    _commerceSession.emit(updatedSessionEvent)
                }
            }
    }

    private suspend fun saveBrandSession(session: CommerceSession.Data?) {
        session?.toBrandSession()?.let { interactor.saveBrandSession(it) }
    }

    private fun getDuration(date: String): Duration {
        val serverTime = date.toDate().toInstant()
        val clientTime = Instant.now()
        val duration = Duration.between(serverTime, clientTime)
        Log.d(null, "getDuration: server: $serverTime")
        Log.d(null, "getDuration: local : $clientTime")
        Log.d(null, "getDuration: >${duration.toMillis()}<")
        return duration
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
