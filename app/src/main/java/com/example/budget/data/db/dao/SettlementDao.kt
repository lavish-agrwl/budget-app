package com.example.budget.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budget.data.db.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    @Delete
    suspend fun deleteSettlement(settlement: SettlementEntity)

    @Query("SELECT * FROM settlements WHERE personId = :personId ORDER BY timestamp ASC")
    fun getSettlementsForPerson(personId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE transactionId = :transactionId ORDER BY timestamp ASC")
    fun getSettlementsForTransaction(transactionId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements ORDER BY timestamp ASC")
    fun getAllSettlements(): Flow<List<SettlementEntity>>
}
