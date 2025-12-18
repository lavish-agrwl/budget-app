package com.example.budget.ui.borrowlend

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budget.data.db.entity.BorrowLendTransactionEntity
import com.example.budget.data.db.entity.SettlementEntity
import com.example.budget.viewmodel.BorrowLendViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    viewModel: BorrowLendViewModel,
    onNavigateBack: () -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val personBalance = uiState.peopleBalances.find { it.person.id == personId }
    
    var showFullSettlementDialog by remember { mutableStateOf(false) }
    var transactionToSettle by remember { mutableStateOf<BorrowLendTransactionEntity?>(null) }
    var transactionToDeleteId by remember { mutableStateOf<Long?>(null) }
    var settlementToDelete by remember { mutableStateOf<SettlementEntity?>(null) }

    LaunchedEffect(personId) {
        viewModel.selectPerson(personId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(personBalance?.person?.name ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Balance Header
            personBalance?.let {
                BalanceHeader(it) { showFullSettlementDialog = true }
            }

            // History List
            Text(
                text = "History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = uiState.selectedPersonHistory,
                    key = { item ->
                        when(item) {
                            is BorrowLendTransactionEntity -> "trans_${item.id}"
                            is SettlementEntity -> "sett_${item.id}"
                            else -> item.hashCode()
                        }
                    }
                ) { item ->
                    when (item) {
                        is BorrowLendTransactionEntity -> {
                            SwipeableTransactionItem(
                                transaction = item,
                                onEdit = { onEditTransaction(item.id) },
                                onDelete = { transactionToDeleteId = item.id },
                                onSettleRequested = { transactionToSettle = item }
                            )
                        }
                        is SettlementEntity -> SettlementHistoryItem(
                            settlement = item,
                            onDelete = { settlementToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Full Settlement Dialog
    if (showFullSettlementDialog && personBalance != null) {
        SettlementConfirmationDialog(
            amount = abs(personBalance.netBalance),
            onConfirm = {
                viewModel.addFullSettlement(personId, abs(personBalance.netBalance))
                showFullSettlementDialog = false
            },
            onDismiss = { showFullSettlementDialog = false }
        )
    }

    // Partial (Per-Transaction) Settlement Dialog
    transactionToSettle?.let { transaction ->
        SettlementConfirmationDialog(
            amount = transaction.amount,
            title = "Settle Transaction",
            message = "Record a settlement for this specific transaction of ₹${String.format("%.2f", transaction.amount)}?",
            onConfirm = {
                viewModel.addPartialSettlement(personId, transaction.id, transaction.amount)
                transactionToSettle = null
            },
            onDismiss = { transactionToSettle = null }
        )
    }

    // Delete Transaction Dialog
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

    // Delete Settlement Dialog
    settlementToDelete?.let { settlement ->
        AlertDialog(
            onDismissRequest = { settlementToDelete = null },
            title = { Text("Delete Settlement") },
            text = { Text("Are you sure you want to delete this settlement record of ₹${settlement.amount}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSettlement(settlement)
                        settlementToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { settlementToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    transaction: BorrowLendTransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettleRequested: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSettleRequested()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                }, label = "swipe_color"
            )
            
            val alignment = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            
            val icon = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Check
                else -> null
            }

            val label = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> "Edit"
                SwipeToDismissBoxValue.EndToStart -> "Settle"
                else -> ""
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                        icon?.let { Icon(it, contentDescription = null) }
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        icon?.let { Icon(it, contentDescription = null) }
                    }
                }
            }
        },
        content = {
            TransactionHistoryItem(
                transaction = transaction,
                onDelete = onDelete
            )
        }
    )
}

@Composable
fun BalanceHeader(
    balance: com.example.budget.viewmodel.PersonBalance,
    onSettlementClick: () -> Unit
) {
    val isOwedToMe = balance.netBalance > 0
    val color = if (isOwedToMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    
    Surface(
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isOwedToMe) "Owes You" else "You Owe",
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Text(
                text = "₹${String.format("%.2f", abs(balance.netBalance))}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            if (abs(balance.netBalance) > 0.01) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSettlementClick,
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settle All")
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryItem(
    transaction: BorrowLendTransactionEntity,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val isLent = transaction.direction == "LENT"
    val color = if (isLent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    ListItem(
        headlineContent = { Text(transaction.description) },
        supportingContent = { Text(dateFormatter.format(Date(transaction.timestamp))) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isLent) "+" else "-"} ₹${transaction.amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun SettlementHistoryItem(
    settlement: SettlementEntity,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    
    ListItem(
        headlineContent = { 
            Text(
                if (settlement.settlementType == "FULL") "Full Settlement" else "Partial Settlement",
                fontWeight = FontWeight.SemiBold
            ) 
        },
        supportingContent = { Text(dateFormatter.format(Date(settlement.timestamp))) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${settlement.amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    )
}

@Composable
fun SettlementConfirmationDialog(
    amount: Double,
    title: String = "Full Settlement",
    message: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message ?: "Are you sure you want to record a full settlement of ₹${String.format("%.2f", amount)}? This will clear the balance.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
