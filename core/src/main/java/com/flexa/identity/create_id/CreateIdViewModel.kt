package com.flexa.identity.create_id

import android.app.Activity
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.entity.TokensResponse
import com.flexa.core.sendFlexaReport
import com.flexa.identity.domain.IIdentityInteractor
import com.flexa.identity.main.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class CreateIdViewModel(
    private val interactor: IIdentityInteractor = com.flexa.identity.Identity.identityInteractor
) : ViewModel() {
    val state = MutableStateFlow<State>(State.General)
    private val _progress = MutableStateFlow(false)
    val progress: StateFlow<Boolean> = _progress
    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    fun accounts(context: Context, userData: UserData) {
        viewModelScope.launch {
            val email = userData.email?.trim() ?: ""
            runCatching {
                _progress.value = true

                var country = getCountryByTelephonyManager(context)
                if (country == null) country = getCountryByLocale(context)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                val birthday = dateFormat.format(userData.birthday ?: Date())

                val res = interactor.accounts(
                    firstName = userData.firstName?.trim() ?: "",
                    lastName = userData.lastName?.trim() ?: "",
                    email = email, country = country, birthday = birthday
                )
                res
            }
                .onFailure {
                    _progress.value = false
                }
                .onSuccess { res ->
                    _progress.value = false
                    when (res) {
                        201 -> tokens(email)
                        else -> _error.value = true
                    }
                }
        }
    }

    fun clearError() {
        _error.value = false
    }

    fun sendFlexaReport(context: Context, userData: UserData) {
        if (context is Activity) {
            val data = "Registration Error for ${userData.email}"
            context.sendFlexaReport(data)
        }
    }

    private fun tokens(email: String) {
        viewModelScope.launch {
            runCatching {
                val res = interactor.tokens(email)
                if (res is TokensResponse.Success)
                    interactor.saveEmail(email)
                res
            }.onSuccess { result ->
                try {
                    when (result) {
                        is TokensResponse.Success -> state.value = State.Success(result)
                        is TokensResponse.Error -> state.value = State.Error(result)
                    }
                } finally {
                    _progress.value = false
                }
            }.onFailure {
                _progress.value = false
            }
        }
    }


    private fun getCountryByTelephonyManager(context: Context): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var countryCode = tm.networkCountryIso
        if (countryCode.isNullOrEmpty()) {
            countryCode = tm.simCountryIso
        }
        return countryCode?.uppercase()
    }

    private fun getCountryByLocale(context: Context): String {
        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return locale.country
    }

    sealed class State {
        data object FlexaPrivacy : State()
        data object General : State()
        class Success(val data: TokensResponse.Success) : State()
        class Error(val data: TokensResponse.Error) : State()
    }
}