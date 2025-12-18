package com.example.budget.data.repository

import com.example.budget.data.db.dao.BorrowLendTransactionDao
import com.example.budget.data.db.dao.SettlementDao
import com.example.budget.data.db.entity.BorrowLendTransactionEntity
import com.example.budget.data.db.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

class BorrowLendRepository(
    private val borrowLendTransactionDao: BorrowLendTransactionDao,
    private val settlementDao: SettlementDao
) {

    suspend fun addTransaction(
        personIds: List<Long>,
        amount: Double,
        direction: String, // "BORROWED" or "LENT"
        description: String
    ) {
        val timestamp = System.currentTimeMillis()
        personIds.forEach { personId ->
            val transaction = BorrowLendTransactionEntity(
                personId = personId,
                amount = amount,
                direction = direction,
                description = description,
                timestamp = timestamp,
                isDeleted = false
            )
            borrowLendTransactionDao.insertTransaction(transaction)
        }
    }

    suspend fun importTransaction(transaction: BorrowLendTransactionEntity) {
        borrowLendTransactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: BorrowLendTransactionEntity) {
        borrowLendTransactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transactionId: Long) {
        borrowLendTransactionDao.softDeleteTransaction(transactionId)
    }

    suspend fun addPartialSettlement(personId: Long, transactionId: Long, amount: Double) {
        val settlement = SettlementEntity(
            personId = personId,
            transactionId = transactionId,
            amount = amount,
            settlementType = "PARTIAL",
            timestamp = System.currentTimeMillis()
        )
        settlementDao.insertSettlement(settlement)
    }

    suspend fun addFullSettlement(personId: Long, amount: Double) {
        val settlement = SettlementEntity(
            personId = personId,
            transactionId = null,
            amount = amount,
            settlementType = "FULL",
            timestamp = System.currentTimeMillis()
        )
        settlementDao.insertSettlement(settlement)
    }

    suspend fun addSettlementDirectly(settlement: SettlementEntity) {
        settlementDao.insertSettlement(settlement)
    }

    suspend fun deleteSettlement(settlement: SettlementEntity) {
        settlementDao.deleteSettlement(settlement)
    }

    fun getTransactionsForPerson(personId: Long): Flow<List<BorrowLendTransactionEntity>> {
        return borrowLendTransactionDao.getTransactionsForPerson(personId)
    }

    fun getSettlementsForPerson(personId: Long): Flow<List<SettlementEntity>> {
        return settlementDao.getSettlementsForPerson(personId)
    }

    fun getAllTransactions(): Flow<List<BorrowLendTransactionEntity>> {
        return borrowLendTransactionDao.getAllTransactions()
    }

    fun getAllSettlements(): Flow<List<SettlementEntity>> {
        return settlementDao.getAllSettlements()
    }
}
