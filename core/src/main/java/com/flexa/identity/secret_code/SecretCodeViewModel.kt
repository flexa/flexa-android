package com.flexa.identity.secret_code

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.Flexa
import com.flexa.core.entity.AppAccount
import com.flexa.core.shared.ApiErrorHandler
import com.flexa.core.shared.Asset
import com.flexa.core.shared.FlexaConstants.Companion.RETRY_COUNT
import com.flexa.core.shared.FlexaConstants.Companion.RETRY_DELAY
import com.flexa.core.shared.filterAssets
import com.flexa.identity.domain.IIdentityInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

private const val ASSETS_PAGE_SIZE = 100

internal class SecretCodeViewModel(
    private val interactor: IIdentityInteractor = com.flexa.identity.Identity.identityInteractor,
    val deepLink: String? = null
) : ViewModel() {

    var progress = MutableStateFlow(false)
    var secretCode = MutableStateFlow<String?>(null)
    val errorHandler = ApiErrorHandler()
    val result = MutableSharedFlow<List<AppAccount>>()
    private var errorMagicCode: String? = null

    init {
        deepLink?.let { loginWithDeepLink(it) }
    }

    internal fun loginWithMagicCode(magicCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            flow {
                interactor.tokenPatch(code = magicCode)

                val assets = try {
                    getAndSaveAssets()
                } catch (e: Exception) {
                    emptyList()
                }

                val acc = interactor.putAppAccounts(Flexa.appAccounts.value.filterAssets(assets))
                interactor.saveAppAccounts(acc.accounts)

                kotlin.runCatching { retrieveAndSaveBrands() }

                emit(acc.accounts)
            }.retryWhen { _, attempt ->
                delay(RETRY_DELAY)
                attempt < RETRY_COUNT
            }.onStart { progress.value = true }
                .catch {
                    Log.e(null, "loginWithMagicCode: ", it)
                    errorMagicCode = magicCode
                    progress.value = false
                    errorHandler.setError(it)
                }.collect { res -> result.emit(res) }
        }
    }

    private fun loginWithDeepLink(deepLink: String) {
        viewModelScope.launch(Dispatchers.IO) {
            flow {
                interactor.tokenPatch(link = deepLink)

                val assets = try {
                    getAndSaveAssets()
                } catch (e: Exception) {
                    emptyList()
                }

                val acc = interactor.putAppAccounts(Flexa.appAccounts.value.filterAssets(assets))
                interactor.saveAppAccounts(acc.accounts)

                kotlin.runCatching { retrieveAndSaveBrands() }

                emit(acc.accounts)
            }.retryWhen { _, attempt ->
                delay(RETRY_DELAY)
                attempt < RETRY_COUNT
            }.onStart { progress.value = true }
                .catch {
                    Log.e(null, "loginWithDeepLink: ", it)
                    progress.value = false
                    errorHandler.setError(it)
                }.collect { res -> result.emit(res) }
        }
    }

    private suspend fun getAndSaveAssets(): List<Asset> {
        val assets = interactor.getAllAssets(ASSETS_PAGE_SIZE)
        interactor.deleteAssets()
        interactor.saveAssets(assets)
        return assets
    }

    private suspend fun retrieveAndSaveBrands() {
        val brands = interactor.getBrands(true)
        interactor.deleteBrands()
        interactor.saveBrands(brands)
    }
}
