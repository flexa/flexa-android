package com.flexa.core

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import com.flexa.core.data.db.DbRepository
import com.flexa.core.data.rest.RestRepository
import com.flexa.core.data.rest.RestRepository.Companion.json
import com.flexa.core.data.storage.SecuredPreferences
import com.flexa.core.domain.db.DbInteractor
import com.flexa.core.domain.rest.RestInteractor
import com.flexa.core.entity.error.SDKInitializationError
import com.flexa.core.shared.AppAccount
import com.flexa.core.shared.FlexaClientConfiguration
import com.flexa.core.shared.FlexaConstants
import com.flexa.core.shared.SelectedAsset
import com.flexa.core.shared.SerializerProvider
import com.flexa.core.theme.FlexaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID


/**
 * The main Flexa access object
 */
@SuppressLint("StaticFieldLeak")
object Flexa {

    /**
     * Application context
     */
    var context: Context? = null
    val scope = CoroutineScope(Job())
    val requiredContext: Context
        get() {
            if (context == null)
                throw SDKInitializationError()
            return context!!
        }
    private val _selectedAsset = MutableStateFlow<SelectedAsset?>(null)
    val selectedAsset: StateFlow<SelectedAsset?> = _selectedAsset
    internal var themeConfig = FlexaTheme()
    private val _appAccounts = MutableStateFlow<List<AppAccount>>(emptyList())
    val appAccounts: StateFlow<List<AppAccount>> = _appAccounts
    private val serializerProvider = SerializerProvider()
    private val preferences by lazy {
        SecuredPreferences(
            context as Application,
            serializerProvider, FlexaConstants.FILE
        )
    }
    private val restRepository by lazy {
        RestRepository(preferences)
    }

    private val dbRepository by lazy {
        DbRepository(requiredContext)
    }

    val restInteractor by lazy(LazyThreadSafetyMode.NONE) {
        RestInteractor(restRepository)
    }

    val dbInteractor by lazy(LazyThreadSafetyMode.NONE) {
        DbInteractor(dbRepository)
    }

    fun init(config: FlexaClientConfiguration) {
        context = config.context.applicationContext
        themeConfig = config.theme
        setAppAccounts(config.appAccounts)
        setUniqueIdentifier()
        saveApiKeys(
            publishableKey = config.publishableKey,
            webViewThemeConfig = config.webViewThemeConfig
        )
    }

    fun updateAppAccounts(appAccounts: ArrayList<AppAccount>) {
        setAppAccounts(appAccounts)
    }

    fun selectedAsset(appAccountId: String, assetId: String) {
        scope.launch {
            val accounts = preferences.getString(FlexaConstants.APP_ACCOUNTS)
                ?.let { json.decodeFromString<List<com.flexa.core.entity.AppAccount>>(it) }
            val acc = accounts?.firstOrNull { it.accountId == appAccountId }
            val asset = acc?.availableAssets?.firstOrNull { it.assetId == assetId }

            val localAccounts = appAccounts.value.firstOrNull { acc?.accountId == it.accountId }
            val localAsset =
                localAccounts?.availableAssets?.firstOrNull { it.assetId == asset?.assetId }

            val dbAsset = dbInteractor.getAssetsById(assetId).firstOrNull()
            val assetWithData = asset?.copy(assetData = dbAsset, icon = localAsset?.icon)
            if (acc != null && assetWithData != null && !assetWithData.zeroValue()) {
                Log.d(
                    "TAG",
                    "setSelectedAsset: Flexa >>> ${assetWithData.label} [${asset.value?.label}] ${assetWithData.assetData}"
                )
                _selectedAsset.emit(SelectedAsset(acc.accountId, assetWithData))
            }
        }
    }

    private fun setAppAccounts(appAccounts: List<AppAccount>?) {
        scope.launch {
            appAccounts?.let { _appAccounts.emit(it) }
        }
    }

    private fun saveApiKeys(
        publishableKey: String,
        webViewThemeConfig: String? = null,
    ) {
        preferences.run {
            savePublishableKey(publishableKey)
            savePlacesToPayTheme(webViewThemeConfig)
        }
    }

    private fun setUniqueIdentifier() {
        preferences.run {
            val storedUniqueId = getStringSynchronously(FlexaConstants.UNIQUE_IDENTIFIER)
            if (storedUniqueId == null) {
                val uniqueIdentifier = UUID.randomUUID().toString()
                saveUniqueIdentifier(uniqueIdentifier)
            }
        }
    }
}
