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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.components.MetricCard
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.formatDouble
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.IncomeEmerald
import com.example.ui.viewmodel.HisabViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currency = profile?.currencySymbol ?: "৳"

    var selectedTab by remember { mutableStateOf(0) } // 0: This Month, 1: All Time
    val tabs = listOf("চলতি মাস (Monthly)", "সর্বকালীন (All Time)")

    val now = Calendar.getInstance()
    val curM = now.get(Calendar.MONTH)
    val curY = now.get(Calendar.YEAR)

    val currentTransactions = if (selectedTab == 0) {
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.transaction.transactionDate }
            cal.get(Calendar.MONTH) == curM && cal.get(Calendar.YEAR) == curY
        }
    } else {
        transactions
    }

    val totalExpense = currentTransactions.filter { it.transaction.transactionType == TransactionType.EXPENSE.name }
        .sumOf { it.transaction.totalAmount }
    val totalIncome = currentTransactions.filter { it.transaction.transactionType == TransactionType.INCOME.name }
        .sumOf { it.transaction.totalAmount }
    val netSavings = totalIncome - totalExpense

    // Category breakdown
    val categoryTotals = mutableMapOf<String, Double>()
    currentTransactions.filter { it.transaction.transactionType == TransactionType.EXPENSE.name }
        .forEach { tx ->
            val cat = tx.transaction.categoryName ?: "Other"
            categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + tx.transaction.totalAmount
        }

    val sortedCategories = categoryTotals.entries.sortedByDescending { it.value }

    // Shop breakdown
    val shopTotals = mutableMapOf<String, Double>()
    currentTransactions.filter { it.transaction.transactionType == TransactionType.EXPENSE.name }
        .forEach { tx ->
            val s = tx.transaction.shopName?.takeIf { it.isNotBlank() } ?: "Local Market / সাধারণ দোকান"
            shopTotals[s] = (shopTotals[s] ?: 0.0) + tx.transaction.totalAmount
        }
    val sortedShops = shopTotals.entries.sortedByDescending { it.value }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Row
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        // Cash Flow Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "মোট আয়",
                    value = "$currency${formatDouble(totalIncome)}",
                    icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = IncomeEmerald) },
                    modifier = Modifier.weight(1f),
                    accentColor = IncomeEmerald
                )
                MetricCard(
                    title = "মোট ব্যয়",
                    value = "$currency${formatDouble(totalExpense)}",
                    icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRose) },
                    modifier = Modifier.weight(1f),
                    accentColor = ExpenseRose
                )
            }
        }

        item {
            MetricCard(
                title = "নিট সঞ্চয় (Net Savings)",
                value = "$currency${formatDouble(netSavings)}",
                subtitle = if (netSavings >= 0) "পজিটিভ ক্যাশ ফ্লো" else "ব্যয়ের পরিমাণ আয়ের চেয়ে বেশি",
                icon = { Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldDark) },
                modifier = Modifier.fillMaxWidth(),
                accentColor = if (netSavings >= 0) EmeraldDark else ExpenseRose
            )
        }

        // Category breakdown progress bars
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "খাতওয়ারি ব্যয়ের অনুপাত (Category Share)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (sortedCategories.isEmpty()) {
                        Text(
                            text = "কোনো ব্যয়ের তথ্য নেই",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        sortedCategories.forEach { (cat, amount) ->
                            val percent = if (totalExpense > 0) (amount / totalExpense) else 0.0
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "$currency${formatDouble(amount)} (${(percent * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { percent.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = EmeraldDark,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shop-wise breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "দোকান ও মার্চেন্ট ভিত্তিক খরচ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (sortedShops.isEmpty()) {
                        Text(
                            text = "কোনো দোকানের তথ্য নেই",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        sortedShops.take(5).forEach { (shop, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(shop, style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${formatDouble(amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
