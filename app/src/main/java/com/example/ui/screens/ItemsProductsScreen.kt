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
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.viewmodel.HisabViewModel

data class ItemAggregate(
    val normalizedName: String,
    val displayName: String,
    val totalPurchasedQty: Double,
    val totalSpent: Double,
    val avgUnitPrice: Double,
    val minUnitPrice: Double,
    val maxUnitPrice: Double,
    val purchaseCount: Int,
    val unit: String
)

@Composable
fun ItemsProductsScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currency = profile?.currencySymbol ?: "৳"

    var searchQuery by remember { mutableStateOf("") }
    var itemToMerge by remember { mutableStateOf<ItemAggregate?>(null) }
    var mergeTargetName by remember { mutableStateOf("") }

    // Aggregate items by normalized name
    val aggregated = items.groupBy { it.normalizedItemName }
        .map { (norm, list) ->
            val totalQty = list.sumOf { it.quantity }
            val totalSpent = list.sumOf { it.totalPrice }
            val prices = list.map { it.unitPrice }.filter { it > 0 }
            ItemAggregate(
                normalizedName = norm,
                displayName = list.firstOrNull()?.itemName ?: norm,
                totalPurchasedQty = totalQty,
                totalSpent = totalSpent,
                avgUnitPrice = if (prices.isNotEmpty()) prices.average() else 0.0,
                minUnitPrice = prices.minOrNull() ?: 0.0,
                maxUnitPrice = prices.maxOrNull() ?: 0.0,
                purchaseCount = list.size,
                unit = list.firstOrNull()?.unit ?: "pcs"
            )
        }
        .sortedByDescending { it.totalSpent }

    val filtered = aggregated.filter {
        searchQuery.isBlank() || it.displayName.lowercase().contains(searchQuery.lowercase().trim())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("items_products_screen")
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("আইটেমের নাম দিয়ে খুঁজুন...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("search_items_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        // Summary Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${filtered.size} টি অনন্য পণ্য সংরক্ষিত",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "কোনো পণ্য পাওয়া যায়নি",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { agg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = agg.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "মোট ক্রয়: ${agg.purchaseCount} বার • ${agg.totalPurchasedQty} ${agg.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "$currency${formatDouble(agg.totalSpent)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "গড় দর: $currency${formatDouble(agg.avgUnitPrice)} | সর্বনিম্ন: $currency${formatDouble(agg.minUnitPrice)} | সর্বোচ্চ: $currency${formatDouble(agg.maxUnitPrice)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )

                                IconButton(
                                    onClick = {
                                        itemToMerge = agg
                                        mergeTargetName = agg.displayName
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CallMerge,
                                        contentDescription = "Merge Duplicate Item",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Merge Dialog
    if (itemToMerge != null) {
        AlertDialog(
            onDismissRequest = { itemToMerge = null },
            title = { Text("পণ্য মার্জ বা নাম পরিবর্তন") },
            text = {
                Column {
                    Text(
                        text = "যদি \"${itemToMerge?.displayName}\" এবং অন্য কোনো পণ্য একই হয় (যেমন: \"মাছ\" ও \"Fish\"), নতুন মানসম্মত নাম নির্ধারণ করুন:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mergeTargetName,
                        onValueChange = { mergeTargetName = it },
                        label = { Text("নতুন মানসম্মত নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mergeTargetName.isNotBlank() && itemToMerge != null) {
                            viewModel.mergeItem(itemToMerge!!.normalizedName, mergeTargetName)
                            itemToMerge = null
                        }
                    }
                ) {
                    Text("মার্জ সম্পন্ন করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToMerge = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
