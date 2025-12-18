package com.example.budget.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budget.data.db.entity.BorrowLendTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowLendTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BorrowLendTransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: BorrowLendTransactionEntity)

    @Query("UPDATE borrow_lend_transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long)

    @Query("SELECT * FROM borrow_lend_transactions WHERE personId = :personId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsForPerson(personId: Long): Flow<List<BorrowLendTransactionEntity>>

    @Query("SELECT * FROM borrow_lend_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<BorrowLendTransactionEntity>>
}
