package com.example.budget.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budget.util.export.BackupManager
import com.example.budget.viewmodel.AppTheme
import com.example.budget.viewmodel.HomeViewModel
import com.example.budget.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    themeViewModel: ThemeViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAddBorrowLend: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val appTheme by themeViewModel.theme.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBackupOptions by remember { mutableStateOf(false) }

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                BackupManager.importFullBackup(context, it)
                // Optionally show a success message or trigger a refresh
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Dashboard") },
                actions = {
                    IconButton(onClick = { showBackupOptions = true }) {
                        Icon(Icons.Default.Backup, contentDescription = "Backup/Restore")
                    }
                    IconButton(onClick = { themeViewModel.toggleTheme() }) {
                        Icon(
                            imageVector = when (appTheme) {
                                AppTheme.LIGHT -> Icons.Default.LightMode
                                AppTheme.DARK -> Icons.Default.DarkMode
                                AppTheme.SYSTEM -> Icons.Default.SettingsBrightness
                            },
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Monthly Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HighlightCard(
                        title = "Income",
                        amount = uiState.totalIncome,
                        icon = Icons.Default.ArrowUpward,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        title = "Expenses",
                        amount = uiState.totalExpenses,
                        icon = Icons.Default.ArrowDownward,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                HighlightCard(
                    title = "Net Balance",
                    amount = uiState.netBalance,
                    icon = Icons.Default.AccountBalanceWallet,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Debts & Loans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HighlightCard(
                        title = "Receivable",
                        amount = uiState.totalLent,
                        icon = Icons.Default.CallReceived,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    HighlightCard(
                        title = "Payable",
                        amount = uiState.totalBorrowed,
                        icon = Icons.Default.CallMade,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                val isNetReceivable = uiState.netBorrowLend >= 0
                HighlightCard(
                    title = if (isNetReceivable) "Net Receivable" else "Net Payable",
                    amount = abs(uiState.netBorrowLend),
                    icon = if (isNetReceivable) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    color = if (isNetReceivable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToAddExpense,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Expense")
                    }
                    OutlinedButton(
                        onClick = onNavigateToAddBorrowLend,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Debt")
                    }
                }
            }
        }
    }

    if (showBackupOptions) {
        AlertDialog(
            onDismissRequest = { showBackupOptions = false },
            title = { Text("Backup & Restore") },
            text = { Text("Create a full backup of all your data (Expenses, Income, Debts, Loans) or restore from a previously saved file.") },
            confirmButton = {
                TextButton(onClick = {
                    showBackupOptions = false
                    scope.launch {
                        BackupManager.createFullBackup(context)
                    }
                }) {
                    Text("Create Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupOptions = false
                    backupPickerLauncher.launch("application/json")
                }) {
                    Text("Restore Backup")
                }
            }
        )
    }
}

@Composable
fun HighlightCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = color)
            }
            Text(
                text = "₹${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
