package com.example.budget.ui.expenses

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.budget.util.export.CSVExporter
import com.example.budget.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    navController: NavController,
    onNavigateToAdd: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var transactionToDeleteId by remember { mutableStateOf<Long?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.prepareImport(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses & Income") },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val categoryMap = uiState.categories.associate { it.id to it.name }
                            CSVExporter.exportExpenses(context, uiState.transactions, categoryMap)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            ExpenseFilterBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { 
                    viewModel.updateFilters(uiState.filterType, uiState.filterCategoryId, it) 
                },
                selectedType = uiState.filterType,
                onTypeSelected = { 
                    viewModel.updateFilters(it, uiState.filterCategoryId, uiState.searchQuery) 
                },
                categories = uiState.categories,
                selectedCategoryId = uiState.filterCategoryId,
                onCategorySelected = { 
                    viewModel.updateFilters(uiState.filterType, it, uiState.searchQuery) 
                }
            )

            if (uiState.transactions.isEmpty()) {
                EmptyState(
                    title = "No transactions yet",
                    description = "Add your first expense or income to get started",
                    icon = Icons.Default.ReceiptLong,
                    onAction = onNavigateToAdd,
                    actionLabel = "Add Transaction"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        val categoryName = uiState.categories.find { it.id == transaction.categoryId }?.name
                        SwipeableExpenseItem(
                            transaction = transaction,
                            categoryName = categoryName,
                            onEdit = { id -> navController.navigate("expense_graph/add?transactionId=$id") },
                            onDelete = { id -> transactionToDeleteId = id }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    transactionToDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { transactionToDeleteId = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(id)
                        transactionToDeleteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Preview Dialog
    uiState.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            title = { Text("Import Preview") },
            text = {
                Column {
                    Text("Total rows: ${preview.totalRows}")
                    Text("Valid transactions: ${preview.transactions.size}")
                    if (preview.invalidRows > 0) {
                        Text("Invalid rows: ${preview.invalidRows}", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("New categories will be created automatically.")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) {
                    Text("Confirm Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImport() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    onAction: (() -> Unit)? = null,
    actionLabel: String? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            if (onAction != null && actionLabel != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAction) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}
