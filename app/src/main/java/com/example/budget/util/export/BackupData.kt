package com.example.budget.util.export

import com.example.budget.data.db.entity.*

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val expenseCategories: List<ExpenseCategoryEntity>,
    val expenseTransactions: List<ExpenseTransactionEntity>,
    val persons: List<PersonEntity>,
    val borrowLendTransactions: List<BorrowLendTransactionEntity>,
    val settlements: List<SettlementEntity>
)
