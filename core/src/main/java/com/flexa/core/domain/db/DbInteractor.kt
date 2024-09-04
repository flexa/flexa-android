package com.flexa.core.domain.db

import com.flexa.core.data.db.TransactionBundle
import com.flexa.core.shared.Asset
import com.flexa.core.shared.Brand


class DbInteractor(
    private val repository: IDbRepository
) {

    suspend fun getAssets(): List<Asset> = repository.getAssets()
    suspend fun getAssetsById(vararg ids: String): List<Asset> =
        repository.getAssetsById(*ids)

    suspend fun deleteAssets() = repository.deleteAssets()
    suspend fun saveAssets(items: List<Asset>) = repository.saveAssets(items)
    suspend fun getBrands(): List<Brand> = repository.getBrands()
    suspend fun saveBrands(items: List<Brand>) = repository.saveBrands(items)
    suspend fun deleteBrands() = repository.deleteBrands()
    suspend fun getTransactionBySessionId(sessionId: String) =
        repository.getTransactionBySessionId(sessionId)

    suspend fun deleteTransaction(sessionId: String) = repository.deleteTransactions(sessionId)
    suspend fun deleteOutdatedTransactions() = repository.deleteOutdatedTransactions()
    suspend fun saveTransaction(transactionBundle: TransactionBundle) =
        repository.saveTransaction(transactionBundle)
}