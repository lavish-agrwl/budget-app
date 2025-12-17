package com.example.budget.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["categoryId"]),
        Index(value = ["type"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ExpenseTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: Long?,
    val description: String,
    val timestamp: Long,
    val isDeleted: Boolean = false
)
