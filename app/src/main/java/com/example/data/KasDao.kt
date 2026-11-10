package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KasDao {
    @Query("SELECT * FROM kas WHERE grup = :grup ORDER BY tanggal ASC, id ASC")
    fun getTransactionsForGroup(grup: String): Flow<List<KasTransaction>>

    @Query("SELECT DISTINCT grup FROM kas")
    fun getAllGroups(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: KasTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: KasTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: KasTransaction)

    @Query("DELETE FROM kas WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<Long>)

    @Query("SELECT * FROM kas WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): KasTransaction?
}
