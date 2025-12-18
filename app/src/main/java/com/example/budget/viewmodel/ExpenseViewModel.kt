package com.example.budget.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget.data.db.entity.ExpenseCategoryEntity
import com.example.budget.data.db.entity.ExpenseTransactionEntity
import com.example.budget.data.repository.ExpenseCategoryRepository
import com.example.budget.data.repository.ExpenseTransactionRepository
import com.example.budget.util.export.CSVImporter
import com.example.budget.util.export.ImportPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseUiState(
    val transactions: List<ExpenseTransactionEntity> = emptyList(),
    val categories: List<ExpenseCategoryEntity> = emptyList(),
    val filterType: String? = null,
    val filterCategoryId: Long? = null,
    val searchQuery: String = "",
    val importPreview: ImportPreview? = null,
    val lastUsedType: String = "EXPENSE",
    val lastUsedCategoryId: Long? = null
)

class ExpenseViewModel(
    private val transactionRepo: ExpenseTransactionRepository,
    private val categoryRepo: ExpenseCategoryRepository
) : ViewModel() {

    private val _filterType = MutableStateFlow<String?>(null)
    private val _filterCategoryId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    private val _lastUsedType = MutableStateFlow("EXPENSE")
    private val _lastUsedCategoryId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<ExpenseUiState> = combine(
        transactionRepo.getAllTransactions(),
        categoryRepo.getActiveCategories(),
        _filterType,
        _filterCategoryId,
        combine(_searchQuery, _importPreview, _lastUsedType, _lastUsedCategoryId) { query, preview, lastType, lastCatId -> 
            listOf(query, preview, lastType, lastCatId)
        }
    ) { transactions, categories, type, categoryId, queryPreviewLast ->
        val query = queryPreviewLast[0] as String
        val preview = queryPreviewLast[1] as ImportPreview?
        val lastType = queryPreviewLast[2] as String
        val lastCatId = queryPreviewLast[3] as Long?
        
        val filteredTransactions = transactions.filter { transaction ->
            val matchesType = type == null || transaction.type == type
            val matchesCategory = categoryId == null || transaction.categoryId == categoryId
            val matchesQuery = query.isEmpty() || transaction.description.contains(query, ignoreCase = true)
            matchesType && matchesCategory && matchesQuery
        }
        ExpenseUiState(
            transactions = filteredTransactions,
            categories = categories,
            filterType = type,
            filterCategoryId = categoryId,
            searchQuery = query,
            importPreview = preview,
            lastUsedType = lastType,
            lastUsedCategoryId = lastCatId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState()
    )

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepo.addCategory(name)
        }
    }

    fun addExpense(amount: Double, categoryId: Long, description: String) {
        viewModelScope.launch {
            transactionRepo.addTransaction(amount, "EXPENSE", categoryId, description)
            _lastUsedType.value = "EXPENSE"
            _lastUsedCategoryId.value = categoryId
        }
    }

    fun addIncome(amount: Double, description: String) {
        viewModelScope.launch {
            transactionRepo.addTransaction(amount, "INCOME", null, description)
            _lastUsedType.value = "INCOME"
        }
    }

    fun updateTransaction(transaction: ExpenseTransactionEntity) {
        viewModelScope.launch {
            transactionRepo.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepo.deleteTransaction(id)
        }
    }

    fun restoreTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepo.restoreTransaction(id)
        }
    }

    fun updateFilters(type: String?, categoryId: Long?, query: String) {
        _filterType.value = type
        _filterCategoryId.value = categoryId
        _searchQuery.value = query
    }

    fun prepareImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            val preview = CSVImporter.parseExpenses(context, uri)
            _importPreview.value = preview
        }
    }

    fun confirmImport() {
        val preview = _importPreview.value ?: return
        viewModelScope.launch {
            preview.transactions.forEachIndexed { index, transaction ->
                val categoryName = preview.categories.getOrNull(index)
                val categoryId = if (!categoryName.isNullOrBlank() && transaction.type == "EXPENSE") {
                    categoryRepo.addCategory(categoryName)
                } else null
                
                transactionRepo.importTransaction(transaction.copy(categoryId = categoryId))
            }
            _importPreview.value = null
        }
    }

    fun cancelImport() {
        _importPreview.value = null
    }
}
