package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionRow
import com.example.ui.theme.EmeraldDark
import com.example.ui.viewmodel.HisabViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val profile by viewModel.profile.collectAsState()

    val currency = profile?.currencySymbol ?: "৳"
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredList = allTransactions.filter { txWithItems ->
        val tx = txWithItems.transaction
        val matchesType = when (selectedType) {
            "ALL" -> true
            else -> tx.transactionType == selectedType
        }
        val query = searchQuery.trim().lowercase()
        val matchesQuery = query.isEmpty() ||
                (tx.shopName?.lowercase()?.contains(query) == true) ||
                (tx.categoryName?.lowercase()?.contains(query) == true) ||
                (tx.notes.lowercase().contains(query)) ||
                (txWithItems.items.any { it.itemName.lowercase().contains(query) })

        matchesType && matchesQuery
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldDark,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("দোকান, আইটেম বা বিবরণ খুঁজুন...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_transactions_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Type filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    "ALL" to "সব লেনদেন",
                    TransactionType.EXPENSE.name to "ব্যয় / Expense",
                    TransactionType.INCOME.name to "আয় / Income",
                    TransactionType.TRANSFER.name to "স্থানান্তর / Transfer"
                )
                items(types) { (type, label) ->
                    val isSelected = selectedType == type
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_tx_type_$type"),
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Results summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredList.size} টি লেনদেন পাওয়া গেছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Transaction list
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "কোনো লেনদেন পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.transaction.id }) { tx ->
                        TransactionRow(
                            txWithItems = tx,
                            currencySymbol = currency,
                            onDelete = { viewModel.deleteTransaction(tx.transaction.id) }
                        )
                    }
                }
            }
        }
    }

    // Manual Add Dialog
    if (showAddDialog) {
        var addType by remember { mutableStateOf("EXPENSE") }
        var amountText by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Groceries") }
        var shopName by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var dateStr by remember {
            mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন লেনদেন যুক্ত করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { addType = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (addType == "EXPENSE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("ব্যয়", color = if (addType == "EXPENSE") androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { addType = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (addType == "INCOME") EmeraldDark else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("আয়", color = if (addType == "INCOME") androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("পরিমাণ ($currency)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_manual_amount"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("দোকান / প্রতিষ্ঠানের নাম") },
                        modifier = Modifier.fillMaxWidth().testTag("input_manual_shop"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("বিবরণ / আইটেমের বিবরণ") },
                        modifier = Modifier.fillMaxWidth().testTag("input_manual_notes")
                    )

                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.saveManualTransaction(
                                type = addType,
                                amount = amt,
                                category = selectedCat,
                                account = accounts.firstOrNull(),
                                shopName = shopName,
                                notes = notes,
                                dateString = dateStr
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_manual_tx")
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
