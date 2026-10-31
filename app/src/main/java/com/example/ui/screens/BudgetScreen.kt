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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType
import com.example.ui.components.formatDouble
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HisabViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun BudgetScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.budgets.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val currency = profile?.currencySymbol ?: "৳"
    val overallMonthlyBudget = profile?.monthlyBudget ?: 35000.0

    var showAddBudgetDialog by remember { mutableStateOf(false) }

    val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    // Calculate this month's spending per category
    val now = Calendar.getInstance()
    val curM = now.get(Calendar.MONTH)
    val curY = now.get(Calendar.YEAR)

    val spendingByCategory = mutableMapOf<String, Double>()
    var totalMonthExpense = 0.0

    allTransactions.filter { tx ->
        val cal = Calendar.getInstance().apply { timeInMillis = tx.transaction.transactionDate }
        cal.get(Calendar.MONTH) == curM && cal.get(Calendar.YEAR) == curY && tx.transaction.transactionType == TransactionType.EXPENSE.name
    }.forEach { tx ->
        val cat = tx.transaction.categoryName ?: "Other"
        spendingByCategory[cat] = (spendingByCategory[cat] ?: 0.0) + tx.transaction.totalAmount
        totalMonthExpense += tx.transaction.totalAmount
    }

    val overallProgress = if (overallMonthlyBudget > 0) (totalMonthExpense / overallMonthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
    val remainingBudget = overallMonthlyBudget - totalMonthExpense

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBudgetDialog = true },
                containerColor = EmeraldDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Monthly Target Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "চলতি মাসের সামগ্রিক বাজেট",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "মাস: $currentMonthYear",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$currency${formatDouble(totalMonthExpense)} / $currency${formatDouble(overallMonthlyBudget)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBudget < 0) ExpenseRose else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = if (remainingBudget < 0) ExpenseRose else EmeraldDark,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ব্যবহৃত: ${(overallProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (remainingBudget >= 0) "অবশিষ্ট: $currency${formatDouble(remainingBudget)}" else "অতিরিক্ত খরচ: $currency${formatDouble(-remainingBudget)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (remainingBudget >= 0) EmeraldDark else ExpenseRose
                            )
                        }

                        if (remainingBudget < 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "সতর্কতা: আপনি মাসিক ব্যয়ের সীমা অতিক্রম করেছেন!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseRose,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Category-wise Budgets Section Header
            item {
                Text(
                    text = "খাতওয়ারি বাজেট সীমা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (budgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "কোনো খাতওয়ারি বাজেট নির্ধারণ করা হয়নি",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showAddBudgetDialog = true }) {
                                Text("নির্দিষ্ট খাতে বাজেট নির্ধারণ করুন")
                            }
                        }
                    }
                }
            } else {
                items(budgets) { b ->
                    val spent = spendingByCategory[b.categoryName] ?: 0.0
                    val prog = if (b.limitAmount > 0) (spent / b.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                    val over = spent > b.limitAmount

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = b.categoryName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$currency${formatDouble(spent)} / $currency${formatDouble(b.limitAmount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (over) ExpenseRose else MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteBudget(b.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRose, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { prog },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (over) ExpenseRose else EmeraldDark,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddBudgetDialog) {
        var selectedCat by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Groceries") }
        var limitText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = { Text("খাতভিত্তিক বাজেট নির্ধারণ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = { selectedCat = it },
                        label = { Text("খাতের নাম (যেমন: Groceries, Transport)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("মাসিক সীমা ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = limitText.toDoubleOrNull() ?: 0.0
                        if (limit > 0) {
                            viewModel.saveBudget(
                                categoryId = selectedCat,
                                categoryName = selectedCat,
                                limit = limit,
                                monthYear = currentMonthYear
                            )
                            showAddBudgetDialog = false
                        }
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddBudgetDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
