package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class AccountType {
    CASH,
    BANK,
    BKASH,
    NAGAD,
    ROCKET,
    CREDIT_CARD
}

enum class TransactionSource {
    CHAT_AI,
    RECEIPT_SCANNER,
    MANUAL
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String = "user_default",
    val name: String = "User",
    val email: String = "user@hisab.ai",
    val currency: String = "BDT",
    val currencySymbol: String = "৳",
    val language: String = "bn", // "bn" or "en"
    val monthlyBudget: Double = 35000.0,
    val isDarkMode: Boolean = false
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // CASH, BANK, BKASH, etc.
    val balance: Double = 0.0,
    val currency: String = "BDT",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameBn: String,
    val type: String = "EXPENSE", // EXPENSE or INCOME
    val icon: String = "shopping_cart",
    val colorHex: String = "#10B981"
)

@Entity(
    tableName = "shops",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class ShopEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val normalizedName: String,
    val address: String = "",
    val phone: String = "",
    val visitCount: Int = 1,
    val totalSpent: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("transactionDate"),
        Index("shopId"),
        Index("accountId"),
        Index("categoryId"),
        Index("transactionType")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionType: String, // EXPENSE, INCOME, TRANSFER
    val transactionDate: Long, // Epoch millis
    val dateString: String, // YYYY-MM-DD
    val shopId: String? = null,
    val shopName: String? = null,
    val accountId: String? = null,
    val destinationAccountId: String? = null, // for TRANSFER
    val categoryId: String? = null,
    val categoryName: String? = null,
    val totalAmount: Double,
    val currency: String = "BDT",
    val paymentMethod: String = "Cash",
    val notes: String = "",
    val rawMessage: String = "",
    val source: String = TransactionSource.MANUAL.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transaction_items",
    indices = [
        Index("transactionId"),
        Index("normalizedItemName"),
        Index("categoryId")
    ]
)
data class TransactionItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val itemName: String,
    val normalizedItemName: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val unitPrice: Double,
    val totalPrice: Double,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val notes: String = ""
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "monthYear"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val categoryName: String,
    val monthYear: String, // e.g. "2026-10"
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String? = null,
    val imageUri: String = "",
    val merchantName: String = "",
    val totalAmount: Double = 0.0,
    val dateString: String = "",
    val rawOcrText: String = "",
    val isProcessed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_api_keys")
data class AiApiKeyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val provider: String, // "GEMINI" or "OPENAI"
    val apiKey: String,
    val model: String = "gemini-3.8-flash",
    val isEnabled: Boolean = true,
    val priority: Int = 1,
    val requestCount: Int = 0,
    val tokenUsage: Long = 0L,
    val lastError: String? = null,
    val lastTestedAt: Long = 0L,
    val isConnected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_messages")
data class AiMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system"
    val content: String,
    val language: String = "bn",
    val pendingTransactionJson: String? = null,
    val isConfirmed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class TransactionWithItems(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItemEntity>
)
