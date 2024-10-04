package com.flexa.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable

@Dao
internal interface TransactionFeeDao {
    @Query("SELECT * FROM transaction_fee")
    fun getAll(): List<TransactionFee>

    @Query("SELECT * FROM transaction_fee WHERE asset = :id LIMIT 1")
    fun getByAssetId(id: String): TransactionFee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<TransactionFee>)

    @Query("DELETE FROM transaction_fee")
    fun deleteAll()
}


@Entity(tableName = "transaction_fee")
internal class TransactionFee(
    @PrimaryKey
    val asset: String,
    @ColumnInfo(name = "amount")
    val amount: String? = null,
    @ColumnInfo(name = "equivalent")
    val equivalent: String? = null,
    @ColumnInfo(name = "label")
    val label: String? = null,
    @ColumnInfo(name = "price")
    @TypeConverters(ObjectConverters::class)
    val price: TransactionFeePrice? = null,
    @ColumnInfo(name = "unit_of_account")
    val unitOfAccount: String? = null,
    @ColumnInfo(name = "zone")
    val zone: String? = null
)

@Serializable
class TransactionFeePrice(
    @ColumnInfo(name = "amount")
    val amount: String? = null,
    @ColumnInfo(name = "label")
    val label: String? = null,
    @ColumnInfo(name = "priority")
    val priority: String? = null
)

internal fun TransactionFee.toObject(): com.flexa.core.entity.TransactionFee =
    com.flexa.core.entity.TransactionFee(
        asset = asset, amount = amount, equivalent = equivalent,
        label = label, price = com.flexa.core.entity.TransactionFeePrice(
            amount = price?.amount, label = price?.label, priority = price?.priority
        ), unitOfAccount = unitOfAccount, zone = zone
    )

internal fun com.flexa.core.entity.TransactionFee.toDao(): TransactionFee =
    TransactionFee(
        asset = asset, amount = amount, equivalent = equivalent,
        label = label, price = TransactionFeePrice(
            amount = price?.amount, label = price?.label, priority = price?.priority
        ), unitOfAccount = unitOfAccount, zone = zone
    )
