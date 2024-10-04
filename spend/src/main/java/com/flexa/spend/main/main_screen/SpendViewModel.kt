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
import com.flexa.core.entity.ExchangeRate
import com.flexa.core.entity.Notification
import com.flexa.core.entity.OneTimeKey
import com.flexa.core.shared.ApiErrorHandler
import com.flexa.core.shared.Brand
import com.flexa.core.shared.ConnectionState
import com.flexa.core.shared.SelectedAsset
import com.flexa.spend.Spend
import com.flexa.spend.containsAuthorization
import com.flexa.spend.domain.CommerceSessionWorker
import com.flexa.spend.domain.ISpendInteractor
import com.flexa.spend.hasAnotherAsset
import com.flexa.spend.isCompleted
import com.flexa.spend.isValid
import com.flexa.spend.toBrandSession
import com.flexa.spend.toTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val selectedBrand = MutableStateFlow<Brand?>(null)
    private val _progress = MutableStateFlow(false)
    val progress: StateFlow<Boolean> = _progress
    private val _timeout = MutableStateFlow(false)
    val timeout: StateFlow<Boolean> = _timeout
    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account
    private val _notifications = mutableStateListOf<Notification>()
    val notifications: List<Notification> = _notifications

    private val _commerceSession = MutableStateFlow<CommerceSession?>(null)
    val commerceSession = _commerceSession
        .onStart { listenConnection() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(3000),
            initialValue = null
        )

    var sheetScreen by mutableStateOf<SheetScreen>(SheetScreen.Assets)
    val errorHandler = ApiErrorHandler()
    var openLegacyCard = MutableStateFlow(false)

    init {
        listenFlexaAppAccounts()
        listenCommerceSession()
        listenTransactionSent()
        emitCachedAppAccount()
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
        _commerceSession.value = null
    }

    private var brandSessionId: String? = null

    internal fun createCommerceSession(
        brandId: String, amount: String,
        assetId: String, paymentAssetId: String
    ) {
        viewModelScope.launch {
            flow {
                emit(
                    interactor.createCommerceSession(
                        brandId, amount, assetId, paymentAssetId
                    )
                )
            }
                .filter { it.isValid() }
                .catch { stopProgress() }
                .retryWhen { _, attempt ->
                    delay(RETRY_DELAY)
                    attempt < RETRY_COUNT
                }
                .onStart { _progress.value = true }
                .onEach { session ->
                    brandSessionId = session.id
                    interactor.deleteOutdatedSessions()
                    initCloseSessionTimeout()
                }
                .collect { session ->
                    saveBrandSession(session)
                    val cs = session.copy(isLegacy = true)
                    saveLastSessionId(cs.id)
                    _commerceSession.emit(CommerceSession(data = cs))
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
        cancelTimeout()
        CommerceSessionWorker.execute(context, sessionId)
    }

    internal fun removeNotification(notification: Notification) {
        _notifications.remove(notification)
        viewModelScope.launch {
            interactor.deleteNotification(notification.id ?: "")
        }
    }

    internal suspend fun getAccount() {
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

    private suspend fun isLegacy(
        commerceSession: CommerceSession.Data?
    ): Boolean {
        val brandSession = interactor.getBrandSession(commerceSession?.id ?: "")
        return brandSession != null
    }

    private var listenEventsJob: Job? = null
    private fun listenEvents() {
        listenEventsJob?.cancel()
        listenEventsJob = viewModelScope.launch {
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
                                event.session.data?.let { proceedCommerceSession(it) }
                            }
                        }

                        is CommerceSessionEvent.Updated -> {
                            if (event.session.isValid()) {
                                event.session.data?.let { proceedCommerceSession(it) }
                            }
                        }

                        is CommerceSessionEvent.Completed -> {
                            if (event.session.isValid()) {
                                event.session.data?.let { proceedCommerceSession(it) }
                            }
                        }
                    }
                }
        }
    }

    private suspend fun proceedCommerceSession(session: CommerceSession.Data) {
        if (!session.isLegacy && session.isCompleted()) {
            // Close Next-Gen session card
            deleteCommerceSessionData()
        } else {
            val updatedSessionEvent = CommerceSession(
                data = session.copy(isLegacy = isLegacy(session))
            )
            _commerceSession.emit(updatedSessionEvent)
        }
    }

    private var listenConnectionJob: Job? = null
    private fun listenConnection() {
        if (listenConnectionJob?.isActive == true) {
            listenConnectionJob?.cancel()
        }
        listenConnectionJob = viewModelScope.launch {
            interactor.getConnectionListener()
                ?.distinctUntilChanged()
                ?.filter { it is ConnectionState.Available }
                ?.collect {
                    getAccount()
                    checkLastSession()
                    listenEvents()
                }
        }
    }

    private fun emitCachedAppAccount() {
        viewModelScope.launch {
            eventFlow.emit(Event.AppAccountsUpdate(interactor.getLocalAppAccounts()))
        }
    }

    private fun listenFlexaAppAccounts() {
        viewModelScope.launch {
            Flexa.appAccounts.collect { appAccounts ->
                runCatching {
                    val putAccount = interactor.putAccounts(appAccounts)
                    putAccount.accounts
                }.onSuccess { res ->
                    eventFlow.emit(Event.AppAccountsUpdate(res))
                }
            }
        }
    }

    private fun listenCommerceSession() {
        viewModelScope.launch {
            _commerceSession
                .filter { it != null }
                .filter { it?.isValid() == true }
                .map { it!! }
                .onEach { cs ->
                    val selectedAssetId = selectedAsset.value?.asset?.assetId ?: ""
                    val sessionId = cs.data?.id ?: ""
                    if (cs.data?.hasAnotherAsset(selectedAssetId) == true) {
                        patchCommerceSession(sessionId, selectedAssetId)
                    }
                    saveLastSessionId(sessionId)
                }
                .catch { }
                .collect { cs ->
                    val legacy = cs.data?.isLegacy ?: false
                    val containsAuthorization = cs.containsAuthorization()

                    val inputAmountClosed = selectedBrand.value == null
                    val showBrandPaymentCard =
                        (legacy && !containsAuthorization && inputAmountClosed)
                                || (legacy && containsAuthorization)

                    val sendTransactionRequest = brandSessionId == cs.data?.id
                    if (legacy && sendTransactionRequest) {
                        cs.toTransaction()?.let { transaction ->
                            brandSessionId = null
                            Log.d(null, "CS>>>: onTransactionRequest ${cs.data?.id}")
                            Spend.onTransactionRequest?.invoke(Result.success(transaction))
                        }
                    }

                    if (containsAuthorization) {
                        stopProgress()
                        cancelTimeout()
                    }
                    if (showBrandPaymentCard) {
                        openLegacyCard.value = true
                    }
                }
        }
    }

    private suspend fun patchCommerceSession(
        sessionId: String,
        assetId: String
    ) {
        interactor.patchCommerceSession(
            commerceSessionId = sessionId,
            paymentAssetId = assetId
        )
    }

    private fun listenTransactionSent() {
        if (Flexa.context != null) {
            Spend.transactionSent = { id, txSignature ->
                viewModelScope.launch {
                    interactor.getBrandSession(id)?.let { transaction ->
                        flow {
                            emit(
                                interactor.confirmTransaction(
                                    transaction.transactionId,
                                    txSignature
                                )
                            )
                        }
                            .retryWhen { _, attempt ->
                                delay(RETRY_DELAY)
                                attempt < RETRY_COUNT
                            }
                            .catch { Log.e(null, "listenTransactionSent: ", it) }
                            .collect { }
                    }
                }
            }
        }
    }

    private suspend fun saveLastSessionId(sessionId: String?) {
        interactor.saveLastSessionId(sessionId)
    }

    private fun deleteLastSessionId() {
        viewModelScope.launch { interactor.saveLastSessionId(null) }
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
            .catch { Log.e(null, "checkLastSession: ", it) }
            .filter { it != null }
            .filter { it.isValid() }
            .collect { cs ->
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
}

sealed class SheetScreen {
    data object Assets : SheetScreen()
    class AssetDetails(val asset: SelectedAsset) : SheetScreen()
    data object PlacesToPay : SheetScreen()
    data object Locations : SheetScreen()
    class PaymentDetails(val session: StateFlow<CommerceSession?>) : SheetScreen()
}

sealed class Event {
    class AppAccountsUpdate(val accounts: List<AppAccount>) : Event()
    class ExchangeRatesUpdate(val items: List<ExchangeRate>) : Event()
    class OneTimeKeysUpdate(val items: List<OneTimeKey>) : Event()
}
