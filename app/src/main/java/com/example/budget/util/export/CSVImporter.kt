package com.example.budget.util.export

import android.content.Context
import android.net.Uri
import com.example.budget.data.db.entity.BorrowLendTransactionEntity
import com.example.budget.data.db.entity.ExpenseTransactionEntity
import com.example.budget.data.db.entity.SettlementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class ImportPreview(
    val transactions: List<ExpenseTransactionEntity>,
    val categories: List<String>,
    val invalidRows: Int,
    val totalRows: Int
)

data class BorrowLendImportPreview(
    val transactions: List<Pair<BorrowLendTransactionEntity, String>>, // Entity + Person Name
    val settlements: List<Pair<SettlementEntity, String>>, // Entity + Person Name
    val invalidRows: Int,
    val totalRows: Int
)

object CSVImporter {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun parseExpenses(context: Context, uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        var invalidRows = 0
        var totalRows = 0
        val parsedTransactions = mutableListOf<Pair<ExpenseTransactionEntity, String?>>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readLine() // Skip header
                var line: String? = reader.readLine()
                while (line != null) {
                    totalRows++
                    try {
                        val parts = line.split(",")
                        if (parts.size >= 5) {
                            val timestamp = try { dateFormatter.parse(parts[0])?.time ?: 0L } catch (e: Exception) { 0L }
                            val categoryName = if (parts[1] == "EXPENSE") parts[3] else null
                            val transaction = ExpenseTransactionEntity(
                                amount = parts[2].toDouble(),
                                type = parts[1],
                                categoryId = null,
                                description = parts[4],
                                timestamp = if (timestamp != 0L) timestamp else System.currentTimeMillis()
                            )
                            parsedTransactions.add(transaction to categoryName)
                        } else {
                            invalidRows++
                        }
                    } catch (e: Exception) {
                        invalidRows++
                    }
                    line = reader.readLine()
                }
            }
        }

        ImportPreview(
            transactions = parsedTransactions.map { it.first },
            categories = parsedTransactions.map { it.second ?: "" },
            invalidRows = invalidRows,
            totalRows = totalRows
        )
    }

    suspend fun parseBorrowLend(context: Context, uri: Uri): BorrowLendImportPreview = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Pair<BorrowLendTransactionEntity, String>>()
        val settlements = mutableListOf<Pair<SettlementEntity, String>>()
        var invalidRows = 0
        var totalRows = 0

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readLine() // Skip header
                var line: String? = reader.readLine()
                while (line != null) {
                    totalRows++
                    try {
                        val parts = line.split(",")
                        if (parts.size >= 8) {
                            val personName = parts[0]
                            val direction = parts[1]
                            val amount = parts[2].toDouble()
                            val description = parts[3]
                            val transDateStr = parts[4]
                            val settType = parts[5]
                            val settAmount = parts[6].toDouble()
                            val settDateStr = parts[7]

                            if (direction == "SETTLEMENT") {
                                val timestamp = try { dateFormatter.parse(settDateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                                settlements.add(
                                    SettlementEntity(
                                        personId = 0, // Map in VM
                                        transactionId = null,
                                        amount = settAmount,
                                        settlementType = settType,
                                        timestamp = timestamp
                                    ) to personName
                                )
                            } else {
                                val timestamp = try { dateFormatter.parse(transDateStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                                transactions.add(
                                    BorrowLendTransactionEntity(
                                        personId = 0, // Map in VM
                                        amount = amount,
                                        direction = direction,
                                        description = description,
                                        timestamp = timestamp
                                    ) to personName
                                )
                            }
                        } else {
                            invalidRows++
                        }
                    } catch (e: Exception) {
                        invalidRows++
                    }
                    line = reader.readLine()
                }
            }
        }

        BorrowLendImportPreview(transactions, settlements, invalidRows, totalRows)
    }
}
