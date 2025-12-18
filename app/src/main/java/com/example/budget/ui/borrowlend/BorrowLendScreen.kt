package com.example.budget.ui.borrowlend

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budget.data.repository.BorrowLendRepository
import com.example.budget.util.export.CSVExporter
import com.example.budget.viewmodel.BorrowLendViewModel
import com.example.budget.viewmodel.PersonBalance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowLendScreen(
    viewModel: BorrowLendViewModel,
    borrowLendRepo: BorrowLendRepository,
    onNavigateToAdd: () -> Unit,
    onPersonClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var personToSettle by remember { mutableStateOf<PersonBalance?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.prepareImport(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Borrow & Lend") },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val peopleMap = uiState.peopleBalances.associate { it.person.id to it.person.name }
                            val allTransactions = borrowLendRepo.getAllTransactions().first()
                            val allSettlements = borrowLendRepo.getAllSettlements().first()
                            CSVExporter.exportBorrowLend(context, peopleMap, allTransactions, allSettlements)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            BorrowLendSummaryHeader(
                totalLent = uiState.totalLent,
                totalBorrowed = uiState.totalBorrowed,
                netBorrowLend = uiState.netBorrowLend
            )

            if (uiState.peopleBalances.isEmpty()) {
                EmptyBorrowLendState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.peopleBalances,
                        key = { it.person.id }
                    ) { balance ->
                        if (abs(balance.netBalance) > 0.01) {
                            SwipeablePersonItem(
                                balance = balance,
                                onClick = { onPersonClick(balance.person.id) },
                                onSettleRequested = { personToSettle = balance }
                            )
                        } else {
                            PersonListItem(
                                balance = balance,
                                onClick = { onPersonClick(balance.person.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Import Preview Dialog
    uiState.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            title = { Text("Import Preview") },
            text = {
                Column {
                    Text("Total rows: ${preview.totalRows}")
                    Text("Transactions: ${preview.transactions.size}")
                    Text("Settlements: ${preview.settlements.size}")
                    if (preview.invalidRows > 0) {
                        Text("Invalid rows: ${preview.invalidRows}", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Missing people will be created automatically.")
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

    val currentPersonToSettle = personToSettle
    if (currentPersonToSettle != null) {
        SettlementConfirmationDialog(
            amount = abs(currentPersonToSettle.netBalance),
            onConfirm = {
                viewModel.addFullSettlement(currentPersonToSettle.person.id, abs(currentPersonToSettle.netBalance))
                personToSettle = null
            },
            onDismiss = { personToSettle = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeablePersonItem(
    balance: PersonBalance,
    onClick: () -> Unit,
    onSettleRequested: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onSettleRequested()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }, label = "swipe_color"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Settle",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Settle",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        content = {
            PersonListItem(balance = balance, onClick = onClick)
        }
    )
}

@Composable
fun BorrowLendSummaryHeader(totalLent: Double, totalBorrowed: Double, netBorrowLend: Double) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("You will receive", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "₹${String.format("%.2f", totalLent)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("You owe", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "₹${String.format("%.2f", totalBorrowed)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            val isNetReceivable = netBorrowLend >= 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNetReceivable) "Net Receivable" else "Net Payable",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "₹${String.format("%.2f", abs(netBorrowLend))}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isNetReceivable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PersonListItem(
    balance: PersonBalance,
    onClick: () -> Unit
) {
    val isOwedToMe = balance.netBalance > 0
    val isOwedByMe = balance.netBalance < 0
    val amountText = "₹${String.format("%.2f", abs(balance.netBalance))}"
    
    val color = when {
        isOwedToMe -> MaterialTheme.colorScheme.primary
        isOwedByMe -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val directionText = when {
        isOwedToMe -> "owes you"
        isOwedByMe -> "you owe"
        else -> "settled"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = balance.person.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = directionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.8f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isOwedToMe) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyBorrowLendState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No active debts or loans",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Add a transaction to start tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
