package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AccountEntity
import com.example.data.model.AiApiKeyEntity
import com.example.data.model.AiMessageEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.ReceiptEntity
import com.example.data.model.ShopEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import com.example.data.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface HisabDao {

    // --- Profile ---
    @Query("SELECT * FROM profiles LIMIT 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :id")
    suspend fun adjustAccountBalance(id: String, delta: Double)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    // --- Shops ---
    @Query("SELECT * FROM shops ORDER BY visitCount DESC, name ASC")
    fun getAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE normalizedName = :norm LIMIT 1")
    suspend fun getShopByNormalizedName(norm: String): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Update
    suspend fun updateShop(shop: ShopEntity)

    @Query("DELETE FROM shops WHERE id = :id")
    suspend fun deleteShop(id: String)

    @Query("UPDATE shops SET visitCount = visitCount + 1, totalSpent = totalSpent + :amount WHERE id = :id")
    suspend fun recordShopVisit(id: String, amount: Double)

    // --- Transactions & Items ---
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAt DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE transactionType = :type ORDER BY transactionDate DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionWithItems(id: String): TransactionWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteTransactionItems(transactionId: String)

    @Query("SELECT * FROM transaction_items ORDER BY itemName ASC")
    fun getAllTransactionItems(): Flow<List<TransactionItemEntity>>

    @Query("UPDATE transaction_items SET itemName = :newName, normalizedItemName = :newNormalized WHERE normalizedItemName = :oldNormalized")
    suspend fun mergeItemNames(oldNormalized: String, newNormalized: String, newName: String)

    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY monthYear DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: String)

    // --- Receipts ---
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteReceipt(id: String)

    // --- AI API Keys ---
    @Query("SELECT * FROM ai_api_keys ORDER BY priority ASC, createdAt DESC")
    fun getAllApiKeys(): Flow<List<AiApiKeyEntity>>

    @Query("SELECT * FROM ai_api_keys WHERE isEnabled = 1 ORDER BY priority ASC LIMIT 1")
    suspend fun getActiveApiKey(): AiApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(key: AiApiKeyEntity)

    @Update
    suspend fun updateApiKey(key: AiApiKeyEntity)

    @Query("DELETE FROM ai_api_keys WHERE id = :id")
    suspend fun deleteApiKey(id: String)

    // --- AI Messages ---
    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    fun getAllAiMessages(): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiMessage(message: AiMessageEntity)

    @Update
    suspend fun updateAiMessage(message: AiMessageEntity)

    @Query("DELETE FROM ai_messages")
    suspend fun clearAiMessages()

    @Query("DELETE FROM ai_messages WHERE id = :id")
    suspend fun deleteAiMessage(id: String)

    @Query("DELETE FROM transaction_items WHERE id = :id")
    suspend fun deleteTransactionItemById(id: String)

    @Query("DELETE FROM transaction_items WHERE normalizedItemName = :normalizedName")
    suspend fun deleteItemsByNormalizedName(normalizedName: String)

    @Query("UPDATE transaction_items SET itemName = :newName, unitPrice = :price, totalPrice = quantity * :price WHERE id = :id")
    suspend fun updateTransactionItem(id: String, newName: String, price: Double)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun clearAllTransactionItems()

    @Query("DELETE FROM shops")
    suspend fun clearAllShops()

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()

    @Query("DELETE FROM receipts")
    suspend fun clearAllReceipts()

    @Query("UPDATE accounts SET balance = 0.0")
    suspend fun resetAccountBalances()
}
