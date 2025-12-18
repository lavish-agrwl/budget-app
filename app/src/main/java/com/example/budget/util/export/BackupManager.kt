package com.example.budget.util.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.budget.data.db.BudgetDatabase
import com.example.budget.data.db.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupManager {

    private const val BACKUP_FILE_NAME = "budget_full_backup.json"

    suspend fun createFullBackup(context: Context) {
        withContext(Dispatchers.IO) {
            val db = BudgetDatabase.getDatabase(context)
            
            val backupData = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())
                
                put("expenseCategories", JSONArray(db.expenseCategoryDao().getAllActiveCategories().first().map { it.toJSONObject() }))
                put("expenseTransactions", JSONArray(db.expenseTransactionDao().getAllTransactions().first().map { it.toJSONObject() }))
                put("persons", JSONArray(db.personDao().getAllActivePeople().first().map { it.toJSONObject() }))
                put("borrowLendTransactions", JSONArray(db.borrowLendTransactionDao().getAllTransactions().first().map { it.toJSONObject() }))
                put("settlements", JSONArray(db.settlementDao().getAllSettlements().first().map { it.toJSONObject() }))
            }

            val file = File(context.cacheDir, BACKUP_FILE_NAME)
            FileOutputStream(file).use { 
                it.write(backupData.toString(4).toByteArray())
            }

            shareBackupFile(context, file)
        }
    }

    private fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Export Full Backup"))
    }

    suspend fun importFullBackup(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@withContext
            val json = JSONObject(content)
            
            val db = BudgetDatabase.getDatabase(context)
            
            // Note: This is a destructive import in terms of IDs if not handled carefully.
            // For a "full restore", we might want to clear existing data first or merge.
            // Let's implement a merge/replace strategy.
            
            val categories = json.getJSONArray("expenseCategories")
            for (i in 0 until categories.length()) {
                db.expenseCategoryDao().insertCategory(categories.getJSONObject(i).toExpenseCategoryEntity())
            }

            val expenseTransactions = json.getJSONArray("expenseTransactions")
            for (i in 0 until expenseTransactions.length()) {
                db.expenseTransactionDao().insertTransaction(expenseTransactions.getJSONObject(i).toExpenseTransactionEntity())
            }

            val persons = json.getJSONArray("persons")
            for (i in 0 until persons.length()) {
                db.personDao().insertPerson(persons.getJSONObject(i).toPersonEntity())
            }

            val blTransactions = json.getJSONArray("borrowLendTransactions")
            for (i in 0 until blTransactions.length()) {
                db.borrowLendTransactionDao().insertTransaction(blTransactions.getJSONObject(i).toBorrowLendTransactionEntity())
            }

            val settlements = json.getJSONArray("settlements")
            for (i in 0 until settlements.length()) {
                db.settlementDao().insertSettlement(settlements.getJSONObject(i).toSettlementEntity())
            }
        }
    }

    // Helper extensions for JSON mapping
    private fun ExpenseCategoryEntity.toJSONObject() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isPredefined", isPredefined)
        put("isActive", isActive)
    }

    private fun JSONObject.toExpenseCategoryEntity() = ExpenseCategoryEntity(
        id = getLong("id"),
        name = getString("name"),
        isPredefined = getBoolean("isPredefined"),
        isActive = getBoolean("isActive")
    )

    private fun ExpenseTransactionEntity.toJSONObject() = JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("type", type)
        put("categoryId", categoryId)
        put("description", description)
        put("timestamp", timestamp)
        put("isDeleted", isDeleted)
    }

    private fun JSONObject.toExpenseTransactionEntity() = ExpenseTransactionEntity(
        id = getLong("id"),
        amount = getDouble("amount"),
        type = getString("type"),
        categoryId = if (isNull("categoryId")) null else getLong("categoryId"),
        description = getString("description"),
        timestamp = getLong("timestamp"),
        isDeleted = getBoolean("isDeleted")
    )

    private fun PersonEntity.toJSONObject() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("isMerged", isMerged)
        put("mergedIntoPersonId", mergedIntoPersonId)
    }

    private fun JSONObject.toPersonEntity() = PersonEntity(
        id = getLong("id"),
        name = getString("name"),
        isMerged = getBoolean("isMerged"),
        mergedIntoPersonId = if (isNull("mergedIntoPersonId")) null else getLong("mergedIntoPersonId")
    )

    private fun BorrowLendTransactionEntity.toJSONObject() = JSONObject().apply {
        put("id", id)
        put("personId", personId)
        put("amount", amount)
        put("direction", direction)
        put("description", description)
        put("timestamp", timestamp)
        put("isDeleted", isDeleted)
    }

    private fun JSONObject.toBorrowLendTransactionEntity() = BorrowLendTransactionEntity(
        id = getLong("id"),
        personId = getLong("personId"),
        amount = getDouble("amount"),
        direction = getString("direction"),
        description = getString("description"),
        timestamp = getLong("timestamp"),
        isDeleted = getBoolean("isDeleted")
    )

    private fun SettlementEntity.toJSONObject() = JSONObject().apply {
        put("id", id)
        put("personId", personId)
        put("transactionId", transactionId)
        put("amount", amount)
        put("settlementType", settlementType)
        put("timestamp", timestamp)
    }

    private fun JSONObject.toSettlementEntity() = SettlementEntity(
        id = getLong("id"),
        personId = getLong("personId"),
        transactionId = if (isNull("transactionId")) null else getLong("transactionId"),
        amount = getDouble("amount"),
        settlementType = getString("settlementType"),
        timestamp = getLong("timestamp")
    )
}
