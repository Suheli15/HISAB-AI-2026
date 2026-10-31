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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.AiApiKeyEntity
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.IncomeEmerald
import com.example.ui.viewmodel.HisabViewModel

@Composable
fun ApiKeyManagerScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("api_key_manager_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add API Key", tint = Color.White)
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
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "মাল্টিপল এআই প্রোভাইডার ম্যানেজার",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Google Gemini ও OpenAI API কি কনফিগার করুন। কি যুক্ত না থাকলেও বিল্ট-ইন বাংলা এনএলপি ইঞ্জিন সবসময় অফলাইনেও কার্যকর থাকবে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "কনফিগার করা প্রোভাইডারসমূহ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (apiKeys.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("কোনো API কি যুক্ত করা নেই। ডিফল্ট এনএলপি সক্রিয় রয়েছে।", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(apiKeys) { key ->
                    val maskedKey = if (key.apiKey.length > 8) {
                        "${key.apiKey.take(4)}••••••••${key.apiKey.takeLast(4)}"
                    } else if (key.apiKey.isNotEmpty()) {
                        "••••••••"
                    } else {
                        "ডিফল্ট সার্ভার কি (বা এনএলপি ফলব্যাক)"
                    }

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
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (key.provider == "GEMINI") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF10A37F).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = key.provider,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (key.provider == "GEMINI") MaterialTheme.colorScheme.primary else Color(0xFF10A37F),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = key.model,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Switch(
                                    checked = key.isEnabled,
                                    onCheckedChange = { viewModel.toggleApiKey(key) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldDark)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "API Key: $maskedKey",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "অনুরোধ সংখ্যা: ${key.requestCount} | টোকেন ব্যবহার: ${key.tokenUsage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            if (key.lastError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = ExpenseRose, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(key.lastError, style = MaterialTheme.typography.labelSmall, color = ExpenseRose)
                                }
                            } else if (key.isConnected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeEmerald, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("সংযোগ সফল (Active)", style = MaterialTheme.typography.labelSmall, color = IncomeEmerald)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.testApiKey(key) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("কানেকশন টেস্ট", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteApiKey(key.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRose, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var provider by remember { mutableStateOf("GEMINI") }
        var keyInput by remember { mutableStateOf("") }
        var modelInput by remember { mutableStateOf("gemini-3.8-flash") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন এআই ক্রেডেনশিয়াল যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                provider = "GEMINI"
                                modelInput = "gemini-3.8-flash"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (provider == "GEMINI") EmeraldDark else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("Gemini", color = if (provider == "GEMINI") Color.White else MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = {
                                provider = "OPENAI"
                                modelInput = "gpt-4o-mini"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (provider == "OPENAI") EmeraldDark else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("OpenAI", color = if (provider == "OPENAI") Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth().testTag("input_api_key"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("মডেল নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keyInput.isNotBlank()) {
                            viewModel.saveApiKey(provider, keyInput.trim(), modelInput.trim())
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_api_key")
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
