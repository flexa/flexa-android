package com.flexa.spend.main.assets

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.Flexa
import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.nonZeroAssets
import com.flexa.core.shared.ApiErrorHandler
import com.flexa.core.shared.SelectedAsset
import com.flexa.spend.MockFactory
import com.flexa.spend.Spend
import com.flexa.spend.domain.FakeInteractor
import com.flexa.spend.domain.ISpendInteractor
import com.flexa.spend.getKey
import com.flexa.spend.main.main_screen.Event
import com.flexa.spend.main.main_screen.SpendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssetsViewModel(
    private val interactor: ISpendInteractor = Spend.interactor,
    private val selectedAsset: StateFlow<SelectedAsset?> = MutableStateFlow(null),
) : ViewModel() {

    private val _appAccounts = if (interactor is FakeInteractor)
        mutableStateListOf(MockFactory.getMockConfig().first())
    else
        mutableStateListOf()
    val appAccounts: List<AppAccount> = _appAccounts

    private val _assets = if (interactor is FakeInteractor)
        mutableStateListOf(MockFactory.getMockSelectedAsset())
    else
        mutableStateListOf()
    val assets: List<SelectedAsset> = _assets

    val assetsScreen = MutableStateFlow<AssetsScreen>(AssetsScreen.Assets)
    var filtered = MutableStateFlow(false)
    val assetsState = MutableStateFlow<AssetsState>(AssetsState.Retrieving)
    var filterValue = 0.0
    private val errorHandler = ApiErrorHandler()

    init {
        subscribeAppAccounts()
    }

    internal fun setSelectedAsset(accountId: String, asset: AvailableAsset) {
        if (accountId != selectedAsset.value?.accountId || asset != selectedAsset.value?.asset) {
            Spend.selectedAsset(SelectedAsset(accountId, asset))
        }
    }

    private fun subscribeAppAccounts() {
        viewModelScope.launch {
            SpendViewModel.eventFlow.collect {
                when (it) {
                    is Event.AppAccountsUpdate -> {
                        compileAccountsAssets(it.accounts)
                    }
                }
            }
        }
    }

    private fun compileAccountsAssets(accounts: List<AppAccount>) {
        viewModelScope.launch(Dispatchers.IO) {
            updateAssetsKeys()
            try {
                val dbAssets = interactor.getDbAssets()
                val assets = dbAssets.ifEmpty { interactor.getAllAssets() }
                accounts.forEach { account ->
                    val localAccount =
                        Flexa.appAccounts.value.firstOrNull { it.accountId == account.accountId }
                    account.displayName = localAccount?.displayName
                    val accAssets = ArrayList<AvailableAsset>(account.availableAssets.size)
                    for (availableAsset in account.availableAssets) {
                        val localAsset =
                            localAccount?.availableAssets?.firstOrNull { it.assetId == availableAsset.assetId }
                        val assetData = assets.firstOrNull { it.id == availableAsset.assetId }
                        accAssets.add(
                            availableAsset.copy(
                                assetData = assetData,
                                icon = localAsset?.icon
                            )
                        )
                    }
                    account.availableAssets.clear()
                    account.availableAssets.addAll(accAssets)
                }

                _appAccounts.clear()
                _appAccounts.addAll(accounts)

                compileAssets(accounts)
                verifyAssetState(accounts)
                verifySelectedAsset(accounts)

            } catch (e: Exception) {
                Log.e(null, "checkAccountsAssets: ", e)
                withContext(Dispatchers.Main) {
                    errorHandler.setError(e)
                }
            }
        }
    }

    private fun compileAssets(appAccounts: List<AppAccount>) {
        val list = ArrayList<SelectedAsset>(appAccounts.sumOf { it.availableAssets.size })
        for (appAccount in appAccounts) {
            for (asset in appAccount.availableAssets) {
                val assetKey = appAccounts.getKey(SelectedAsset(appAccount.accountId, asset))
                list.add(SelectedAsset(appAccount.accountId, asset.copy(key = assetKey)))
            }
        }
        _assets.clear()
        _assets.addAll(list)
    }

    private fun verifyAssetState(appAccounts: List<AppAccount>) {
        val assetsSize = appAccounts.sumOf { it.availableAssets.size }
        if (assetsSize == 0)
            assetsState.value = AssetsState.NoAssets(emptyList())
        else
            assetsState.value = AssetsState.Fine(emptyList())
    }

    private fun verifySelectedAsset(appAccounts: List<AppAccount>) {
        when {
            selectedAsset.value == null -> selectFirst(appAccounts)
            selectedAsset.value != null -> checkSelectedAssetRepresented(appAccounts)
        }
    }

    private fun selectFirst(appAccounts: List<AppAccount>) {
        val account = appAccounts.firstOrNull { it.nonZeroAssets().isNotEmpty() }
        val asset = account?.nonZeroAssets()?.firstOrNull()
        asset?.let { setSelectedAsset(account.accountId, it) }
    }

    private fun checkSelectedAssetRepresented(appAccounts: List<AppAccount>) {
        selectedAsset.value?.let { selectedAsset ->
            val account =
                appAccounts.firstOrNull { it.accountId == selectedAsset.accountId }
            val asset =
                account?.nonZeroAssets()?.firstOrNull { it.assetId == selectedAsset.asset.assetId }
            if (asset == null)
                selectFirst(appAccounts)
        }
    }


    private suspend fun updateAssetsKeys() {
        val livemodeAsset = interactor.getAssetWithKey(true)
        val testmodeAsset = interactor.getAssetWithKey(false)
        SpendViewModel.livemodeAsset = livemodeAsset
        SpendViewModel.testmodeAsset = testmodeAsset
    }
}

sealed class AssetsScreen {
    data object Assets : AssetsScreen()
    data class Settings(val asset: SelectedAsset? = null) : AssetsScreen()
    data class AssetDetails(val asset: SelectedAsset) : AssetsScreen()
}
