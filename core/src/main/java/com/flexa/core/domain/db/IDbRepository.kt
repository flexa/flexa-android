package com.flexa.core.domain.db

import com.flexa.core.shared.Asset
import com.flexa.core.shared.Brand


interface IDbRepository {

    suspend fun getAssets(): List<Asset>

    suspend fun getAssetsById(vararg ids: String): List<Asset>

    suspend fun deleteAssets()

    suspend fun saveAssets(items: List<Asset>)

    suspend fun getBrands(): List<Brand>

    suspend fun deleteBrands()

    suspend fun saveBrands(items: List<Brand>)
}
