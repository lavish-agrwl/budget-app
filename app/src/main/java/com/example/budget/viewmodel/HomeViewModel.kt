package com.example.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget.data.repository.BorrowLendRepository
import com.example.budget.data.repository.ExpenseTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import kotlin.math.abs

data class HomeUiState(
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netBorrowLend: Double = 0.0
)

class HomeViewModel(
    private val expenseRepo: ExpenseTransactionRepository,
    private val borrowLendRepo: BorrowLendRepository
) : ViewModel() {

    private val startOfMonth: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    val uiState: StateFlow<HomeUiState> = combine(
        expenseRepo.getAllTransactions(),
        borrowLendRepo.getAllTransactions(),
        borrowLendRepo.getAllSettlements()
    ) { transactions, blTransactions, blSettlements ->
        val currentMonthTransactions = transactions.filter { it.timestamp >= startOfMonth }
        
        val expenses = currentMonthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val income = currentMonthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        
        // Calculate BL totals
        val peopleIds = blTransactions.map { it.personId }.toSet()
        val balances = peopleIds.map { personId ->
            val totalLent = blTransactions.filter { it.personId == personId && it.direction == "LENT" }.sumOf { it.amount }
            val totalBorrowed = blTransactions.filter { it.personId == personId && it.direction == "BORROWED" }.sumOf { it.amount }
            val totalSettled = blSettlements.filter { it.personId == personId }.sumOf { it.amount }
            
            val netTransactions = totalLent - totalBorrowed
            if (netTransactions >= 0) netTransactions - totalSettled else netTransactions + totalSettled
        }

        val totalLentValue = balances.filter { it > 0 }.sumOf { it }
        val totalBorrowedValue = balances.filter { it < 0 }.sumOf { abs(it) }

        HomeUiState(
            totalExpenses = expenses,
            totalIncome = income,
            netBalance = income - expenses,
            totalLent = totalLentValue,
            totalBorrowed = totalBorrowedValue,
            netBorrowLend = totalLentValue - totalBorrowedValue
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
