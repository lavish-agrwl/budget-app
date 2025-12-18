package com.example.budget.ui.borrowlend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.budget.viewmodel.BorrowLendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBorrowLendScreen(
    viewModel: BorrowLendViewModel,
    onNavigateBack: () -> Unit,
    transactionId: Long? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("LENT") } // LENT or BORROWED
    val selectedPersonIds = remember { mutableStateListOf<Long>() }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf("") }

    // Pre-fill data if editing
    LaunchedEffect(transactionId, uiState.selectedPersonHistory) {
        if (transactionId != null) {
            val transaction = uiState.selectedPersonHistory
                .filterIsInstance<com.example.budget.data.db.entity.BorrowLendTransactionEntity>()
                .find { it.id == transactionId }
            
            transaction?.let {
                amount = it.amount.toString()
                description = it.description
                direction = it.direction
                if (!selectedPersonIds.contains(it.personId)) {
                    selectedPersonIds.add(it.personId)
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (transactionId == null) "Add Borrow/Lend" else "Edit Transaction") },
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
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // Direction Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = direction == "LENT",
                        onClick = { direction = "LENT" },
                        label = { Text("Lent") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = direction == "BORROWED",
                        onClick = { direction = "BORROWED" },
                        label = { Text("Borrowed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // People Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select People", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { showAddPersonDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New")
                    }
                }

                if (uiState.peopleBalances.isEmpty()) {
                    Text(
                        "No people added yet. Add someone to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.peopleBalances.forEach { balance ->
                        val isSelected = selectedPersonIds.contains(balance.person.id)
                        ListItem(
                            headlineContent = { Text(balance.person.name) },
                            trailingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        // If editing, usually we only have one person. 
                                        // For simplicity in edit mode, we'll just allow switching.
                                        if (transactionId != null) {
                                            selectedPersonIds.clear()
                                        }
                                        if (checked) selectedPersonIds.add(balance.person.id)
                                        else selectedPersonIds.remove(balance.person.id)
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (transactionId != null) {
                                        selectedPersonIds.clear()
                                    }
                                    if (isSelected) selectedPersonIds.remove(balance.person.id)
                                    else selectedPersonIds.add(balance.person.id)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        if (amountDouble > 0 && selectedPersonIds.isNotEmpty()) {
                            if (transactionId == null) {
                                viewModel.addTransaction(
                                    personIds = selectedPersonIds.toList(),
                                    amount = amountDouble,
                                    direction = direction,
                                    description = description
                                )
                            } else {
                                // Edit mode - assuming single person update
                                val transaction = uiState.selectedPersonHistory
                                    .filterIsInstance<com.example.budget.data.db.entity.BorrowLendTransactionEntity>()
                                    .find { it.id == transactionId }
                                
                                transaction?.let {
                                    viewModel.updateTransaction(it.copy(
                                        amount = amountDouble,
                                        description = description,
                                        direction = direction,
                                        personId = selectedPersonIds.first()
                                    ))
                                }
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amount.isNotEmpty() && selectedPersonIds.isNotEmpty()
                ) {
                    Text(if (transactionId == null) "Save Transaction" else "Update Transaction")
                }
            }
        }
    }

    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("Add Person") },
            text = {
                OutlinedTextField(
                    value = newPersonName,
                    onValueChange = { newPersonName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPersonName.isNotBlank()) {
                            viewModel.addPerson(newPersonName)
                            newPersonName = ""
                            showAddPersonDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
