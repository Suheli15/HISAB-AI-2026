package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.components.MetricCard
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatDouble
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.IncomeEmerald
import com.example.ui.viewmodel.HisabViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncomeScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currency = profile?.currencySymbol ?: "৳"

    var showAddDialog by remember { mutableStateOf(false) }

    val incomeTransactions = allTransactions.filter {
        it.transaction.transactionType == TransactionType.INCOME.name
    }
    val totalIncome = incomeTransactions.sumOf { it.transaction.totalAmount }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("income_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Income", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Income Metric
            MetricCard(
                title = "সর্বমোট অর্জিত আয়",
                value = "$currency${formatDouble(totalIncome)}",
                subtitle = "${incomeTransactions.size} টি আয়ের লেনদেন",
                icon = {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeEmerald)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                accentColor = IncomeEmerald
            )

            Text(
                text = "আয়ের বিস্তারিত তালিকা",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (incomeTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "কোনো আয়ের তথ্য পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "চ্যাট থেকে লিখুন: \"গতকাল বেতন পেয়েছি ২৫,০০০ টাকা\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(incomeTransactions) { tx ->
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

    if (showAddDialog) {
        var amountText by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Salary") }
        var notes by remember { mutableStateOf("") }
        var dateStr by remember {
            mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন আয় যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("টাকার পরিমাণ ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("উৎস (যেমন: Salary, Freelance, Business)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("বিবরণ") },
                        modifier = Modifier.fillMaxWidth()
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
                                type = "INCOME",
                                amount = amt,
                                category = category,
                                account = accounts.firstOrNull(),
                                shopName = "",
                                notes = notes,
                                dateString = dateStr
                            )
                            showAddDialog = false
                        }
                    }
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
