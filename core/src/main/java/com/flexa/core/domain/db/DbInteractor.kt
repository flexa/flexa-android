package com.flexa.core.domain.db

import com.flexa.core.data.db.BrandSession
import com.flexa.core.entity.ExchangeRate
import com.flexa.core.shared.Asset
import com.flexa.core.shared.Brand


class DbInteractor(
    private val repository: IDbRepository
) {

    suspend fun clearAllTables() = repository.clearAllTables()
    suspend fun getAssets(): List<Asset> = repository.getAssets()
    suspend fun getAssetsById(vararg ids: String): List<Asset> =
        repository.getAssetsById(*ids)

    suspend fun deleteAssets() = repository.deleteAssets()
    suspend fun saveAssets(items: List<Asset>) = repository.saveAssets(items)
    suspend fun getBrands(): List<Brand> = repository.getBrands()
    suspend fun saveBrands(items: List<Brand>) = repository.saveBrands(items)
    suspend fun deleteBrands() = repository.deleteBrands()
    suspend fun hasOutdatedExchangeRates(): Boolean = repository.hasOutdatedExchangeRates()
    suspend fun getExchangeRates(): List<ExchangeRate> = repository.getExchangeRates()
    suspend fun saveExchangeRates(items: List<ExchangeRate>) = repository.saveExchangeRates(items)
    suspend fun deleteExchangeRates() = repository.deleteExchangeRates()
    suspend fun getBrandSession(sessionId: String) =
        repository.getBrandSession(sessionId)

    suspend fun deleteBrandSession(sessionId: String) = repository.deleteBrandSession(sessionId)
    suspend fun deleteOutdatedSessions() = repository.deleteOutdatedSessions()
    suspend fun saveTransaction(brandSession: BrandSession) =
        repository.saveTransaction(brandSession)
}
