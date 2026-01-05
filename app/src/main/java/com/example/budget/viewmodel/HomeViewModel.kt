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
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

data class HomeUiState(
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netBorrowLend: Double = 0.0,
    val selectedMonth: YearMonth = YearMonth.now()
)

class HomeViewModel(
    private val expenseRepo: ExpenseTransactionRepository,
    private val borrowLendRepo: BorrowLendRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = combine(
        expenseRepo.getAllTransactions(),
        borrowLendRepo.getAllTransactions(),
        borrowLendRepo.getAllSettlements(),
        _selectedMonth
    ) { transactions, blTransactions, blSettlements, month ->
        
        val monthStart = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEnd = month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val filteredTransactions = transactions.filter { it.timestamp in monthStart..monthEnd }
        
        val expenses = filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val income = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        
        // Borrow/Lend summaries are always lifetime values
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
            netBorrowLend = totalLentValue - totalBorrowedValue,
            selectedMonth = month
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun goToPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        val nextMonth = _selectedMonth.value.plusMonths(1)
        if (nextMonth <= YearMonth.now()) {
            _selectedMonth.value = nextMonth
        }
    }
}
