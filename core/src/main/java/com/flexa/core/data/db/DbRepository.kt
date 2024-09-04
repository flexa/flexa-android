package com.flexa.core.data.db

import android.content.Context
import androidx.room.Room
import com.flexa.BuildConfig
import com.flexa.core.domain.db.IDbRepository
import com.flexa.core.shared.Asset
import com.flexa.core.shared.Brand

class DbRepository(
    context: Context,
) : IDbRepository {

    private val db = Room.databaseBuilder(
        context,
        Database::class.java, BuildConfig.LIBRARY_PACKAGE_NAME
    )
        .fallbackToDestructiveMigration()
        .build()

    override suspend fun getAssets(): List<Asset> =
        db.assetsDao().getAll().map { it.toObject() }

    override suspend fun getAssetsById(vararg ids: String): List<Asset> =
        db.assetsDao().getByIds(*ids).map { it.toObject() }

    override suspend fun saveAssets(items: List<Asset>) =
        db.assetsDao().insertAll(items.map { it.toDao() })

    override suspend fun deleteAssets() = db.assetsDao().deleteAll()

    override suspend fun getBrands(): List<Brand> =
        db.brandsDao().getAll().map { it.toObject() }

    override suspend fun saveBrands(items: List<Brand>) =
        db.brandsDao().insertAll(items.map { it.toDao() })

    override suspend fun getTransactionBySessionId(sessionId: String): TransactionBundle? {
        return db.transactionDao().getBySessionId(sessionId).firstOrNull()
    }

    override suspend fun deleteTransactions(vararg sessionIds: String) {
        db.transactionDao().deleteById(*sessionIds)
    }

    override suspend fun deleteOutdatedTransactions() {
        db.transactionDao().deleteOutdated()
    }

    override suspend fun saveTransaction(transactionBundle: TransactionBundle) {
        db.transactionDao().insert(transactionBundle)
    }

    override suspend fun deleteBrands() = db.brandsDao().deleteAll()
}
