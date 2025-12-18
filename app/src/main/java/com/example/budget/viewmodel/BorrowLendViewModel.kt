package com.example.budget.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budget.data.db.entity.BorrowLendTransactionEntity
import com.example.budget.data.db.entity.PersonEntity
import com.example.budget.data.db.entity.SettlementEntity
import com.example.budget.data.repository.BorrowLendRepository
import com.example.budget.data.repository.PersonRepository
import com.example.budget.util.export.BorrowLendImportPreview
import com.example.budget.util.export.CSVImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

data class PersonBalance(
    val person: PersonEntity,
    val netBalance: Double // Positive means they owe us (LENT), Negative means we owe them (BORROWED)
)

data class BorrowLendUiState(
    val peopleBalances: List<PersonBalance> = emptyList(),
    val selectedPersonHistory: List<Any> = emptyList(), // Mixed list of Transactions and Settlements
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netBorrowLend: Double = 0.0,
    val importPreview: BorrowLendImportPreview? = null
)

class BorrowLendViewModel(
    private val borrowLendRepo: BorrowLendRepository,
    private val personRepo: PersonRepository
) : ViewModel() {

    private val _selectedPersonId = MutableStateFlow<Long?>(null)
    private val _importPreview = MutableStateFlow<BorrowLendImportPreview?>(null)

    val uiState: StateFlow<BorrowLendUiState> = combine(
        personRepo.getActivePeople(),
        borrowLendRepo.getAllTransactions(),
        borrowLendRepo.getAllSettlements(),
        _selectedPersonId,
        _importPreview
    ) { people, allTransactions, allSettlements, selectedId, importPreview ->
        
        val balances = people.map { person ->
            val totalLent = allTransactions.filter { it.personId == person.id && it.direction == "LENT" }.sumOf { it.amount }
            val totalBorrowed = allTransactions.filter { it.personId == person.id && it.direction == "BORROWED" }.sumOf { it.amount }
            
            val netTransactions = totalLent - totalBorrowed
            val netSettlements = allSettlements.filter { it.personId == person.id }.sumOf { it.amount }
            
            val finalBalance = if (netTransactions >= 0) {
                netTransactions - netSettlements
            } else {
                netTransactions + netSettlements
            }

            PersonBalance(person, finalBalance)
        }

        val totalLentValue = balances.filter { it.netBalance > 0 }.sumOf { it.netBalance }
        val totalBorrowedValue = balances.filter { it.netBalance < 0 }.sumOf { abs(it.netBalance) }

        val history = if (selectedId != null) {
            val personTransactions = allTransactions.filter { it.personId == selectedId }
            val personSettlements = allSettlements.filter { it.personId == selectedId }
            (personTransactions + personSettlements).sortedByDescending { 
                when(it) {
                    is BorrowLendTransactionEntity -> it.timestamp
                    is SettlementEntity -> it.timestamp
                    else -> 0L
                }
            }
        } else emptyList()

        BorrowLendUiState(
            peopleBalances = balances,
            selectedPersonHistory = history,
            totalLent = totalLentValue,
            totalBorrowed = totalBorrowedValue,
            netBorrowLend = totalLentValue - totalBorrowedValue,
            importPreview = importPreview
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BorrowLendUiState()
    )

    fun addPerson(name: String) {
        viewModelScope.launch {
            personRepo.addPerson(name)
        }
    }

    fun addTransaction(personIds: List<Long>, amount: Double, direction: String, description: String) {
        viewModelScope.launch {
            borrowLendRepo.addTransaction(personIds, amount, direction, description)
        }
    }

    fun updateTransaction(transaction: BorrowLendTransactionEntity) {
        viewModelScope.launch {
            borrowLendRepo.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            borrowLendRepo.deleteTransaction(id)
        }
    }

    fun addPartialSettlement(personId: Long, transactionId: Long, amount: Double) {
        viewModelScope.launch {
            borrowLendRepo.addPartialSettlement(personId, transactionId, amount)
        }
    }

    fun addFullSettlement(personId: Long, amount: Double) {
        viewModelScope.launch {
            borrowLendRepo.addFullSettlement(personId, amount)
        }
    }

    fun deleteSettlement(settlement: SettlementEntity) {
        viewModelScope.launch {
            borrowLendRepo.deleteSettlement(settlement)
        }
    }
    
    fun selectPerson(id: Long?) {
        _selectedPersonId.value = id
    }

    fun prepareImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            val preview = CSVImporter.parseBorrowLend(context, uri)
            _importPreview.value = preview
        }
    }

    fun confirmImport() {
        val preview = _importPreview.value ?: return
        viewModelScope.launch {
            preview.transactions.forEach { (trans, personName) ->
                val personId = personRepo.addPersonAndGetId(personName)
                borrowLendRepo.importTransaction(trans.copy(personId = personId))
            }
            preview.settlements.forEach { (sett, personName) ->
                val personId = personRepo.addPersonAndGetId(personName)
                borrowLendRepo.addSettlementDirectly(sett.copy(personId = personId))
            }
            _importPreview.value = null
        }
    }

    fun cancelImport() {
        _importPreview.value = null
    }
}
