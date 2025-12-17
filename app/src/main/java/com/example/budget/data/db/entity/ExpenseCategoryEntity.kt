package com.example.budget.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isPredefined: Boolean = false,
    val isActive: Boolean = true
)
