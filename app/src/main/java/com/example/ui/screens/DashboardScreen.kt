package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.components.MetricCard
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatDouble
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.TransferIndigo
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.HisabViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HisabViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReceiptScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val selectedFilter by viewModel.selectedDateFilter.collectAsState()

    val currency = profile?.currencySymbol ?: "৳"
    val monthlyBudget = profile?.monthlyBudget ?: 35000.0

    // Filter transactions based on date filter
    val filteredTransactions = allTransactions.filter { tx ->
        val cal = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.transaction.transactionDate }

        when (selectedFilter) {
            DateFilterType.TODAY -> {
                cal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) &&
                        cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            }
            DateFilterType.THIS_WEEK -> {
                cal.get(Calendar.WEEK_OF_YEAR) == txCal.get(Calendar.WEEK_OF_YEAR) &&
                        cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            }
            DateFilterType.THIS_MONTH -> {
                cal.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) &&
                        cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            }
            DateFilterType.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.get(Calendar.MONTH) == txCal.get(Calendar.MONTH) &&
                        cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            }
            DateFilterType.THIS_YEAR -> {
                cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR)
            }
            DateFilterType.ALL -> true
        }
    }

    // Balances
    val totalBalance = accounts.sumOf { it.balance }
    val totalIncome = filteredTransactions.filter { it.transaction.transactionType == TransactionType.INCOME.name }
        .sumOf { it.transaction.totalAmount }
    val totalExpense = filteredTransactions.filter { it.transaction.transactionType == TransactionType.EXPENSE.name }
        .sumOf { it.transaction.totalAmount }
    val remainingBudget = monthlyBudget - totalExpense
    val budgetProgress = if (monthlyBudget > 0) (totalExpense / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f

    // Category breakdown
    val categorySpending = mutableMapOf<String, Double>()
    filteredTransactions.filter { it.transaction.transactionType == TransactionType.EXPENSE.name }
        .forEach { tx ->
            val cat = tx.transaction.categoryName ?: "Other"
            categorySpending[cat] = (categorySpending[cat] ?: 0.0) + tx.transaction.totalAmount
        }

    // Top 5 categories for bar chart
    val topCategories = categorySpending.entries
        .sortedByDescending { it.value }
        .take(5)
        .map { Pair(it.key.take(8), it.value) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Quick Action
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_banner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "স্বাগতম, ${profile?.name ?: "User"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "HISAB AI • Your Money. Your Insights. Your AI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToChat,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_ai_chat"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("এআই চ্যাট হিসাব", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onNavigateToReceiptScanner,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_quick_receipt_scan"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("রসিদ স্ক্যান", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Date Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    DateFilterType.TODAY to "আজকের",
                    DateFilterType.THIS_WEEK to "এই সপ্তাহের",
                    DateFilterType.THIS_MONTH to "চলতি মাসের",
                    DateFilterType.LAST_MONTH to "গত মাসের",
                    DateFilterType.THIS_YEAR to "এই বছরের",
                    DateFilterType.ALL to "সকল লেনদেন"
                )
                items(filters) { (type, label) ->
                    val isSelected = selectedFilter == type
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDateFilter(type) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("filter_chip_${type.name}"),
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Financial Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "মোট ব্যালেন্স",
                    value = "$currency${formatDouble(totalBalance)}",
                    subtitle = "${accounts.size} টি একাউন্টে",
                    icon = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = EmeraldDark,
                    testTag = "metric_total_balance"
                )

                MetricCard(
                    title = "মোট ব্যয়",
                    value = "$currency${formatDouble(totalExpense)}",
                    subtitle = "${filteredTransactions.count { it.transaction.transactionType == TransactionType.EXPENSE.name }} টি লেনদেন",
                    icon = {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRose, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = ExpenseRose,
                    testTag = "metric_total_expense"
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "মোট আয়",
                    value = "$currency${formatDouble(totalIncome)}",
                    subtitle = "আয়ের হিসাব",
                    icon = {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeEmerald, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = IncomeEmerald,
                    testTag = "metric_total_income"
                )

                MetricCard(
                    title = "অবশিষ্ট বাজেট",
                    value = "$currency${formatDouble(remainingBudget)}",
                    subtitle = "${(budgetProgress * 100).toInt()}% ব্যবহৃত",
                    icon = {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = WarningAmber,
                    testTag = "metric_remaining_budget"
                )
            }
        }

        // Monthly Budget Progress Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_progress_card")
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মাসিক বাজেট ব্যবহার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$currency${formatDouble(totalExpense)} / $currency${formatDouble(monthlyBudget)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (budgetProgress > 0.9f) ExpenseRose else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (budgetProgress > 0.9f) ExpenseRose else EmeraldDark,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Top Spending Categories Chart
        if (topCategories.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_chart_card")
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "শীর্ষ ব্যয়ের খাতসমূহ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SimpleBarChart(data = topCategories, currencySymbol = currency)
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক লেনদেন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "সব দেখুন (${filteredTransactions.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .testTag("btn_see_all_transactions")
                        .padding(4.dp)
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এই ফিল্টারে কোনো লেনদেন পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToChat,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("এআই দিয়ে খরচ যুক্ত করুন")
                        }
                    }
                }
            }
        } else {
            items(filteredTransactions.take(6)) { tx ->
                TransactionRow(
                    txWithItems = tx,
                    currencySymbol = currency,
                    onDelete = { viewModel.deleteTransaction(tx.transaction.id) }
                )
            }
        }
    }
}
