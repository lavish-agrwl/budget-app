package com.example.budget.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budget.data.db.entity.ExpenseTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: ExpenseTransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: ExpenseTransactionEntity)

    @Query("UPDATE expense_transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long)

    @Query("UPDATE expense_transactions SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreTransaction(id: Long)

    @Query("SELECT * FROM expense_transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<ExpenseTransactionEntity>>

    @Query("SELECT * FROM expense_transactions WHERE type = :type AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<ExpenseTransactionEntity>>

    @Query("SELECT * FROM expense_transactions WHERE description LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY timestamp DESC")
    fun searchTransactions(query: String): Flow<List<ExpenseTransactionEntity>>
}
