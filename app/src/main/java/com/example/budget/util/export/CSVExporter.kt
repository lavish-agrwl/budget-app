package com.example.budget.util.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.budget.data.db.entity.BorrowLendTransactionEntity
import com.example.budget.data.db.entity.ExpenseTransactionEntity
import com.example.budget.data.db.entity.SettlementEntity
import com.example.budget.data.db.entity.PersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CSVExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun exportExpenses(
        context: Context,
        transactions: List<ExpenseTransactionEntity>,
        categories: Map<Long, String>
    ) {
        withContext(Dispatchers.IO) {
            val csvContent = StringBuilder()
            csvContent.append("Date,Type,Amount,Category,Description\n")

            transactions.forEach {
                csvContent.append("${dateFormatter.format(Date(it.timestamp))},")
                csvContent.append("${it.type},")
                csvContent.append("${it.amount},")
                csvContent.append("${if (it.type == "EXPENSE") categories[it.categoryId] ?: "" else ""},")
                csvContent.append("${it.description.replace(",", " ")}\n")
            }

            shareFile(context, "expenses_export.csv", csvContent.toString())
        }
    }

    suspend fun exportBorrowLend(
        context: Context,
        people: Map<Long, String>,
        transactions: List<BorrowLendTransactionEntity>,
        settlements: List<SettlementEntity>
    ) {
        withContext(Dispatchers.IO) {
            val csvContent = StringBuilder()
            csvContent.append("Person Name,Direction,Amount,Description,Transaction Date,Settlement Type,Settlement Amount,Settlement Date\n")

            // For simplicity, we'll list transactions and their settlements separately or flattened
            // Here we flatten them: each line is either a transaction or a settlement
            
            transactions.forEach { trans ->
                csvContent.append("${people[trans.personId] ?: "Unknown"},")
                csvContent.append("${trans.direction},")
                csvContent.append("${trans.amount},")
                csvContent.append("${trans.description.replace(",", " ")},")
                csvContent.append("${dateFormatter.format(Date(trans.timestamp))},")
                csvContent.append("NONE,0,N/A\n")
            }

            settlements.forEach { sett ->
                csvContent.append("${people[sett.personId] ?: "Unknown"},")
                csvContent.append("SETTLEMENT,")
                csvContent.append("0,") // Not a new transaction amount
                csvContent.append("Settlement Payment,")
                csvContent.append("N/A,")
                csvContent.append("${sett.settlementType},")
                csvContent.append("${sett.amount},")
                csvContent.append("${dateFormatter.format(Date(sett.timestamp))}\n")
            }

            shareFile(context, "borrow_lend_export.csv", csvContent.toString())
        }
    }

    private fun shareFile(context: Context, fileName: String, content: String) {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Export CSV"))
    }
}
