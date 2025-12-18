package com.example.budget.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budget.data.db.dao.*
import com.example.budget.data.db.entity.*

@Database(
    entities = [
        ExpenseCategoryEntity::class,
        ExpenseTransactionEntity::class,
        PersonEntity::class,
        BorrowLendTransactionEntity::class,
        SettlementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun expenseTransactionDao(): ExpenseTransactionDao
    abstract fun personDao(): PersonDao
    abstract fun borrowLendTransactionDao(): BorrowLendTransactionDao
    abstract fun settlementDao(): SettlementDao

    companion object {
        @Volatile
        private var INSTANCE: BudgetDatabase? = null

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
