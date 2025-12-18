package com.example.budget.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budget.data.db.entity.ExpenseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: ExpenseCategoryEntity)

    @Query("SELECT * FROM expense_categories WHERE isActive = 1")
    fun getAllActiveCategories(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): ExpenseCategoryEntity?

    @Query("SELECT * FROM expense_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): ExpenseCategoryEntity?
}
