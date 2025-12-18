package com.example.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.budget.data.repository.BorrowLendRepository
import com.example.budget.data.repository.ExpenseCategoryRepository
import com.example.budget.data.repository.ExpenseTransactionRepository
import com.example.budget.data.repository.PersonRepository

class HomeViewModelFactory(
    private val expenseRepo: ExpenseTransactionRepository,
    private val borrowLendRepo: BorrowLendRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(expenseRepo, borrowLendRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ExpenseViewModelFactory(
    private val transactionRepo: ExpenseTransactionRepository,
    private val categoryRepo: ExpenseCategoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(transactionRepo, categoryRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class BorrowLendViewModelFactory(
    private val borrowLendRepo: BorrowLendRepository,
    private val personRepo: PersonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BorrowLendViewModel::class.java)) {
            return BorrowLendViewModel(borrowLendRepo, personRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
