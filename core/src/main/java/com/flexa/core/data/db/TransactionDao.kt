package com.flexa.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Dao
internal interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE session_id IN (:sessionId)")
    fun getBySessionId(sessionId: String): List<TransactionBundle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: TransactionBundle)

    @Query("DELETE FROM transactions")
    fun deleteAll()

    @Query("DELETE FROM transactions WHERE transactionId IN (:sessionId)")
    fun deleteById(vararg sessionId: String)

    @Query("DELETE FROM transactions WHERE date < strftime('%s', 'now')")
    fun deleteOutdated()
}

@Entity(tableName = "transactions")
class TransactionBundle(
    @PrimaryKey
    val transactionId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "date")
    val date: Long,
)
