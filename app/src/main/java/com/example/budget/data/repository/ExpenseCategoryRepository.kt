package com.example.budget.data.repository

import com.example.budget.data.db.dao.ExpenseCategoryDao
import com.example.budget.data.db.entity.ExpenseCategoryEntity
import kotlinx.coroutines.flow.Flow

class ExpenseCategoryRepository(private val expenseCategoryDao: ExpenseCategoryDao) {

    suspend fun addCategory(name: String, isPredefined: Boolean = false): Long {
        val existing = expenseCategoryDao.getCategoryByName(name)
        if (existing != null) return existing.id
        
        val category = ExpenseCategoryEntity(
            name = name,
            isPredefined = isPredefined,
            isActive = true
        )
        return expenseCategoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: ExpenseCategoryEntity) {
        expenseCategoryDao.updateCategory(category)
    }

    suspend fun deactivateCategory(categoryId: Long) {
        val category = expenseCategoryDao.getCategoryById(categoryId)
        category?.let {
            expenseCategoryDao.updateCategory(it.copy(isActive = false))
        }
    }

    fun getActiveCategories(): Flow<List<ExpenseCategoryEntity>> {
        return expenseCategoryDao.getAllActiveCategories()
    }

    suspend fun getCategoryById(id: Long): ExpenseCategoryEntity? {
        return expenseCategoryDao.getCategoryById(id)
    }
}
