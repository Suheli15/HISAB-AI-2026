package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.formatDouble
import com.example.ui.theme.EmeraldDark
import com.example.ui.viewmodel.HisabViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreen(
    viewModel: HisabViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val items by viewModel.items.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currency = profile?.currencySymbol ?: "৳"

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    fun generateTransactionCsv(): String {
        val sb = StringBuilder()
        sb.append("ID,Type,Date,Category,Shop,PaymentMethod,Amount,Currency,Notes\n")
        transactions.forEach { tx ->
            val t = tx.transaction
            sb.append("\"${t.id}\",\"${t.transactionType}\",\"${t.dateString}\",\"${t.categoryName ?: ""}\",\"${t.shopName ?: ""}\",\"${t.paymentMethod}\",\"${t.totalAmount}\",\"${t.currency}\",\"${t.notes.replace("\"", "'")}\"\n")
        }
        return sb.toString()
    }

    fun generateItemsCsv(): String {
        val sb = StringBuilder()
        sb.append("ID,TransactionID,ItemName,Quantity,Unit,UnitPrice,TotalPrice,Category\n")
        items.forEach { itm ->
            sb.append("\"${itm.id}\",\"${itm.transactionId}\",\"${itm.itemName}\",\"${itm.quantity}\",\"${itm.unit}\",\"${itm.unitPrice}\",\"${itm.totalPrice}\",\"${itm.categoryName ?: ""}\"\n")
        }
        return sb.toString()
    }

    fun generateFinancialSummary(): String {
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val totalExp = transactions.filter { it.transaction.transactionType == "EXPENSE" }.sumOf { it.transaction.totalAmount }
        val totalInc = transactions.filter { it.transaction.transactionType == "INCOME" }.sumOf { it.transaction.totalAmount }

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("HISAB AI - ব্যক্তিগত আর্থিক সারাংশ রিপোর্ট\n")
        sb.append("তারিখ: $dateStr\n")
        sb.append("ব্যবহারকারী: ${profile?.name ?: "User"}\n")
        sb.append("মুদ্রা: $currency (${profile?.currency ?: "BDT"})\n")
        sb.append("=========================================\n\n")
        sb.append("১. সামগ্রিক হিসাব:\n")
        sb.append("• মোট আয়: $currency${formatDouble(totalInc)}\n")
        sb.append("• মোট ব্যয়: $currency${formatDouble(totalExp)}\n")
        sb.append("• নিট সঞ্চয়: $currency${formatDouble(totalInc - totalExp)}\n")
        sb.append("• লেনদেনের সংখ্যা: ${transactions.size} টি\n\n")
        sb.append("২. সাম্প্রতিক লেনদেন তালিকা:\n")
        transactions.take(15).forEach { tx ->
            val t = tx.transaction
            sb.append("• [${t.dateString}] ${t.transactionType}: $currency${formatDouble(t.totalAmount)} - ${t.shopName ?: t.categoryName ?: "লেনদেন"}\n")
        }
        sb.append("\n=========================================\n")
        sb.append("Generated by Hisab AI - Your Money. Your Insights. Your AI.\n")
        return sb.toString()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("export_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "এক্সপোর্ট সেন্টার (Export Center)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "আপনার আর্থিক সকল তথ্য CSV, স্প্রেডশিট বা টেক্সট ফরম্যাটে ব্যাকআপ ও শেয়ার করুন।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Export 1: Full Transactions CSV
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
                        text = "সকল লেনদেন এক্সপোর্ট (Transactions CSV)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "মোট ${transactions.size} টি লেনদেন। এক্সেল বা গুগল শিটসে সরাসরি ওপেন করার উপযোগী CSV ফাইল ফরম্যাট।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val csv = generateTransactionCsv()
                                copyToClipboard(csv, "Transactions CSV")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_copy_transactions_csv")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV কপি করুন")
                        }

                        OutlinedButton(
                            onClick = {
                                val csv = generateTransactionCsv()
                                shareText(csv, "Share Transactions CSV")
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার করুন")
                        }
                    }
                }
            }
        }

        // Export 2: Items & Products CSV
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
                        text = "পণ্য ও আইটেম তালিকা (Items CSV)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "মোট ${items.size} টি ক্রয়কৃত পণ্যের দর, পরিমাণ ও খরচের ডাটা।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val csv = generateItemsCsv()
                            copyToClipboard(csv, "Items CSV")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("আইটেম CSV কপি করুন")
                    }
                }
            }
        }

        // Export 3: Formatted Financial Report
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
                        text = "আর্থিক সারাংশ রিপোর্ট (Financial Summary)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "একটি পূর্ণাঙ্গ প্রিন্টেবল টেক্সট সারাংশ যা হোয়াটসঅ্যাপ বা ইমেইলে অনায়াসে পাঠানো যায়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val rep = generateFinancialSummary()
                                copyToClipboard(rep, "Financial Summary")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_copy_financial_summary")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("রিপোর্ট কপি করুন")
                        }

                        OutlinedButton(
                            onClick = {
                                val rep = generateFinancialSummary()
                                shareText(rep, "Share Hisab AI Summary")
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার")
                        }
                    }
                }
            }
        }
    }
}
