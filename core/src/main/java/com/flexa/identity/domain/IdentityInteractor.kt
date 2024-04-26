package com.flexa.identity.domain

import com.flexa.core.data.data.PikSeeProvider
import com.flexa.core.data.rest.RestRepository.Companion.json
import com.flexa.core.data.storage.SecuredPreferences
import com.flexa.core.domain.db.DbInteractor
import com.flexa.core.domain.rest.RestInteractor
import com.flexa.core.entity.PutAppAccountsResponse
import com.flexa.core.entity.TokenPatch
import com.flexa.core.entity.TokensResponse
import com.flexa.core.shared.AppAccount
import com.flexa.core.shared.Asset
import com.flexa.core.shared.AssetsResponse
import com.flexa.core.shared.Brand
import com.flexa.core.shared.FlexaConstants
import com.flexa.identity.create_id.AccountsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class IdentityInteractor(
    private val restInteractor: RestInteractor,
    private val dbInteractor: DbInteractor,
    private val preferences: SecuredPreferences
) : IIdentityInteractor {

    override suspend fun saveEmail(email: String) {
        preferences.saveString(
            FlexaConstants.EMAIL,
            email
        )
    }

    override suspend fun getEmail(): String? {
        return preferences
            .getString(FlexaConstants.EMAIL)
    }

    override suspend fun clearLoginData() = withContext(Dispatchers.IO) {
        deleteAssets()
        deleteBrands()
        preferences.remove(FlexaConstants.EMAIL)
        preferences.remove(FlexaConstants.TOKEN)
        preferences.remove(FlexaConstants.TOKEN_ID)
        preferences.remove(FlexaConstants.VERIFIER)
        preferences.remove(FlexaConstants.TOKEN_EXPIRATION)
        preferences.remove(FlexaConstants.APP_ACCOUNTS)
        preferences.remove(FlexaConstants.ASSET_KEY)
        preferences.remove(FlexaConstants.ASSET_TESTMODE_KEY)
    }

    override suspend fun getAllAssets(pageSize: Int): List<Asset> {
        var startingAfter: String? = null
        val assets = arrayListOf<Asset>()
        do {
            val response = getAssets(pageSize, startingAfter)
            startingAfter = response.startingAfter
            assets.addAll(response.data)
        } while (startingAfter != null)
        return assets
    }

    override suspend fun getAssets(pageSize: Int, nextPageToken: String?): AssetsResponse =
        restInteractor.getAssets(pageSize, nextPageToken)

    override suspend fun deleteAssets() = dbInteractor.deleteAssets()

    override suspend fun saveAssets(items: List<Asset>) =
        dbInteractor.saveAssets(items)

    override suspend fun getBrands(legacyOnly: Boolean?): List<Brand> {
        var startingAfter: String? = null
        val brands = ArrayList<Brand>()
        do {
            val response = restInteractor.getBrands(legacyOnly, startingAfter)
            startingAfter = response.startingAfter
            brands.addAll(response.data)
        } while (startingAfter != null)
        return brands
    }

    override suspend fun saveBrands(items: List<Brand>) =
        dbInteractor.saveBrands(items)

    override suspend fun deleteBrands() = dbInteractor.deleteBrands()

    override suspend fun tokens(email: String): TokensResponse = withContext(Dispatchers.IO) {
        val verifier = PikSeeProvider.getCodeVerifier()
        val challenge = PikSeeProvider.getCodeChallenge(verifier)
        val res = restInteractor.tokens(email, challenge)
        if (res is TokensResponse.Success) {
            preferences.saveString(FlexaConstants.VERIFIER, verifier)
            preferences.saveString(FlexaConstants.TOKEN_ID, res.id)
        }
        res
    }

    override suspend fun accounts(
        firstName: String, lastName: String,
        email: String, country: String,
        birthday: String
    ): Int = withContext(Dispatchers.IO) {
        val request = AccountsRequest(
            country = country,
            dateOfBirth = birthday,
            email = email,
            familyName = firstName,
            givenName = lastName,
        )
        val res = restInteractor.accounts(request)
        res
    }

    override suspend fun tokenPatch(code: String?, link: String?): TokenPatch =
        withContext(Dispatchers.IO) {
            val tokenId = preferences.getString(FlexaConstants.TOKEN_ID) ?: ""
            val verifier = preferences.getString(FlexaConstants.VERIFIER) ?: ""
            val newVerifier = PikSeeProvider.getCodeVerifier()
            val challenge = PikSeeProvider.getCodeChallenge(newVerifier)
            val res = restInteractor.tokenPatch(
                id = tokenId, verifier = verifier, challenge = challenge,
                code = code, link = link
            )
            preferences.saveString(FlexaConstants.VERIFIER, newVerifier)
            preferences.saveString(FlexaConstants.TOKEN, res.value)
            preferences.saveLong(FlexaConstants.TOKEN_EXPIRATION, res.expiration)
            res
        }

    override suspend fun getAppAccounts(): List<com.flexa.core.entity.AppAccount>? {
        val data = preferences.getString(FlexaConstants.APP_ACCOUNTS)
        val accounts =
            data?.let { Json.decodeFromString<List<com.flexa.core.entity.AppAccount>>(it) }
        return accounts
    }

    override suspend fun putAppAccounts(account: List<AppAccount>): PutAppAccountsResponse =
        withContext(Dispatchers.IO) {
            val response = restInteractor.putAccounts(account)
            response
        }

    override suspend fun saveAppAccounts(account: List<com.flexa.core.entity.AppAccount>) {
        val accounts = json.encodeToString(account)
        preferences.saveString(FlexaConstants.APP_ACCOUNTS, accounts)
    }
}
