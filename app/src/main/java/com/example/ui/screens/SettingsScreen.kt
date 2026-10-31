package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.components.formatDouble
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.ExpenseRose
import com.example.ui.viewmodel.HisabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var nameInput by remember(profile) { mutableStateOf(profile?.name ?: "Nahid Hossain") }
    var budgetInput by remember(profile) { mutableStateOf((profile?.monthlyBudget ?: 35000.0).toString()) }
    var selectedCurrency by remember(profile) { mutableStateOf(profile?.currency ?: "BDT") }
    var selectedSymbol by remember(profile) { mutableStateOf(profile?.currencySymbol ?: "৳") }
    var selectedLanguage by remember(profile) { mutableStateOf(profile?.language ?: "bn") }

    var showAddAccountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ব্যবহারকারীর প্রোফাইল",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("নাম") },
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_name"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("মাসিক মোট বাজেট ($selectedSymbol)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_monthly_budget"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val b = budgetInput.toDoubleOrNull() ?: 35000.0
                            viewModel.updateProfile(nameInput, selectedCurrency, selectedSymbol, selectedLanguage, b)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_save_profile")
                    ) {
                        Text("প্রোফাইল আপডেট করুন")
                    }
                }
            }
        }

        // Currency & Locale
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মুদ্রা ও ভাষা নির্বাচন (Currency & Language)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ডিফল্ট মুদ্রা:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currencies = listOf("BDT" to "৳", "USD" to "$", "EUR" to "€", "GBP" to "£")
                        currencies.forEach { (curr, sym) ->
                            val isSel = selectedCurrency == curr
                            ElevatedFilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedCurrency = curr
                                    selectedSymbol = sym
                                    val b = budgetInput.toDoubleOrNull() ?: 35000.0
                                    viewModel.updateProfile(nameInput, curr, sym, selectedLanguage, b)
                                },
                                label = { Text("$curr ($sym)") },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("অ্যাপের ভাষা:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedFilterChip(
                            selected = selectedLanguage == "bn",
                            onClick = {
                                selectedLanguage = "bn"
                                val b = budgetInput.toDoubleOrNull() ?: 35000.0
                                viewModel.updateProfile(nameInput, selectedCurrency, selectedSymbol, "bn", b)
                            },
                            label = { Text("বাংলা") },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        ElevatedFilterChip(
                            selected = selectedLanguage == "en",
                            onClick = {
                                selectedLanguage = "en"
                                val b = budgetInput.toDoubleOrNull() ?: 35000.0
                                viewModel.updateProfile(nameInput, selectedCurrency, selectedSymbol, "en", b)
                            },
                            label = { Text("English") },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Accounts List & Management
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "হিসাবের একাউন্টসমূহ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { showAddAccountDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("নতুন", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(acc.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${acc.type} • $selectedSymbol${formatDouble(acc.balance)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { viewModel.deleteAccount(acc.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRose, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Demo data reset
        item {
            OutlinedButton(
                onClick = { viewModel.resetDemoData() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ডিফল্ট ডেমো ক্যাটাগরি ও ডাটা রিসেট করুন")
            }
        }
    }

    if (showAddAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accType by remember { mutableStateOf("CASH") }
        var initialBalance by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("নতুন একাউন্ট যুক্ত করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text("একাউন্টের নাম (যেমন: bKash, BRAC Bank)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accType,
                        onValueChange = { accType = it },
                        label = { Text("টাইপ (CASH, BANK, BKASH, NAGAD, CARD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        label = { Text("বর্তমান ব্যালেন্স ($selectedSymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = initialBalance.toDoubleOrNull() ?: 0.0
                        if (accName.isNotBlank()) {
                            viewModel.saveAccount(accName, accType, bal)
                            showAddAccountDialog = false
                        }
                    }
                ) {
                    Text("যুক্ত করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAccountDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
