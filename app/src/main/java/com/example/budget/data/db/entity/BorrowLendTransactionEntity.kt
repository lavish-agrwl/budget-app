package com.example.budget.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "borrow_lend_transactions",
    indices = [
        Index(value = ["personId"]),
        Index(value = ["timestamp"]),
        Index(value = ["direction"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BorrowLendTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val amount: Double,
    val direction: String, // "BORROWED" or "LENT"
    val description: String,
    val timestamp: Long,
    val isDeleted: Boolean = false
)
