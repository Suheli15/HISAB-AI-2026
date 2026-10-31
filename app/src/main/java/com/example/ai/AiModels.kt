package com.example.ai

import com.squareup.moshi.JsonClass
import java.util.UUID

enum class AiIntent {
    CREATE_EXPENSE,
    CREATE_INCOME,
    CREATE_TRANSFER,
    QUERY_FINANCES,
    FOLLOW_UP_UPDATE,
    UNKNOWN
}

@JsonClass(generateAdapter = true)
data class ParsedItem(
    val itemName: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val category: String = "Groceries",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class ParsedTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "EXPENSE", // EXPENSE, INCOME, TRANSFER
    val date: String = "", // YYYY-MM-DD
    val shopName: String? = null,
    val paymentMethod: String = "Cash",
    val currency: String = "BDT",
    val items: List<ParsedItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val categoryName: String = "Groceries"
)

@JsonClass(generateAdapter = true)
data class AiExtractionResult(
    val intent: String, // create_expense, create_income, query, etc.
    val language: String = "bn",
    val requiresConfirmation: Boolean = true,
    val missingFields: List<String> = emptyList(),
    val transaction: ParsedTransaction? = null,
    val replyMessage: String = "",
    val queryAnswer: String? = null
)
