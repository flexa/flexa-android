package com.flexa.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(
    entities = [
        Asset::class, Brand::class, TransactionBundle::class
    ], version = 2,
    exportSchema = false
)
@TypeConverters(StringListConverter::class, LegacyFlexcodeConverter::class)
internal abstract class Database : RoomDatabase() {

    abstract fun assetsDao(): AssetDao

    abstract fun brandsDao(): BrandDao

    abstract fun transactionDao(): TransactionDao
}
