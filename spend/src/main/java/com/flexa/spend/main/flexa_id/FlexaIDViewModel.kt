package com.flexa.spend.main.flexa_id

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.entity.Account
import com.flexa.core.shared.ApiErrorHandler
import com.flexa.spend.domain.ISpendInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import java.net.HttpURLConnection

private const val RETRY_DELAY = 500L
private const val RETRY_COUNT = 3

class FlexaIDViewModel(
    private val interactor: ISpendInteractor = com.flexa.spend.Spend.interactor
) : ViewModel(

) {
    private val _progress = MutableStateFlow(false)
    val progress: StateFlow<Boolean> = _progress
    private val _signOut = MutableStateFlow(false)
    val signOut: StateFlow<Boolean> = _signOut
    private val _deleteAccount = MutableStateFlow(false)
    val deleteAccount: StateFlow<Boolean> = _deleteAccount
    val email = MutableStateFlow("")
    val account = MutableStateFlow<Account?>(null)
    val errorHandler = ApiErrorHandler()

    init {
        getUserEmail()
        getAccount()
    }

    internal fun deleteToken() {
        _progress.value = true
        viewModelScope.launch {
            runCatching {
                interactor.deleteToken()
            }.onFailure {
                _progress.value = false
                errorHandler.setError(it)
            }.onSuccess { code ->
                _progress.value = false
                if (code == HttpURLConnection.HTTP_NO_CONTENT ||
                    code < HttpURLConnection.HTTP_INTERNAL_ERROR
                ) {
                    _signOut.value = true
                } else {
                    errorHandler.setError(IllegalArgumentException())
                }
            }
        }
    }

    internal fun deleteAccount() {
        _progress.value = true
        viewModelScope.launch {
            runCatching { interactor.deleteAccount() }
                .onFailure {
                    _progress.value = false
                    errorHandler.setError(it)
                }.onSuccess {
                    _progress.value = false
                    _deleteAccount.value = true
                }
        }
    }

    private fun getUserEmail() {
        viewModelScope.launch {
            val emailValue = interactor.getEmail()
            emailValue?.let {
                email.emit(it)
            }
        }
    }

    private fun getAccount() {
        viewModelScope.launch {
            flow { emit(interactor.getAccount()) }
                .retryWhen { _, attempt ->
                    delay(RETRY_DELAY)
                    attempt < RETRY_COUNT
                }.flowOn(Dispatchers.IO)
                .catch { ex ->
                    errorHandler.setError(ex)
                }.collect {
                    account.emit(it)
                }
        }
    }
}
