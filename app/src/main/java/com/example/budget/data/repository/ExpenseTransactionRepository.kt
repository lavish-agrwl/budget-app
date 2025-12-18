package com.example.budget.data.repository

import com.example.budget.data.db.dao.ExpenseTransactionDao
import com.example.budget.data.db.entity.ExpenseTransactionEntity
import kotlinx.coroutines.flow.Flow

class ExpenseTransactionRepository(private val expenseTransactionDao: ExpenseTransactionDao) {

    suspend fun addTransaction(
        amount: Double,
        type: String, // "EXPENSE" or "INCOME"
        categoryId: Long?,
        description: String
    ) {
        val transaction = ExpenseTransactionEntity(
            amount = amount,
            type = type,
            categoryId = if (type == "INCOME") null else categoryId,
            description = description,
            timestamp = System.currentTimeMillis(),
            isDeleted = false
        )
        expenseTransactionDao.insertTransaction(transaction)
    }

    suspend fun importTransaction(transaction: ExpenseTransactionEntity) {
        expenseTransactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: ExpenseTransactionEntity) {
        expenseTransactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        expenseTransactionDao.softDeleteTransaction(transactionId)
    }

    suspend fun restoreTransaction(transactionId: Long) {
        expenseTransactionDao.restoreTransaction(transactionId)
    }

    fun getAllTransactions(): Flow<List<ExpenseTransactionEntity>> {
        return expenseTransactionDao.getAllTransactions()
    }

    fun getTransactionsByType(type: String): Flow<List<ExpenseTransactionEntity>> {
        return expenseTransactionDao.getTransactionsByType(type)
    }

    fun searchTransactions(query: String): Flow<List<ExpenseTransactionEntity>> {
        return expenseTransactionDao.searchTransactions(query)
    }
}
