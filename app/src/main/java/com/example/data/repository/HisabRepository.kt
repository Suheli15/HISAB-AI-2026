package com.example.data.repository

import com.example.data.local.HisabDao
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
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class HisabRepository(private val dao: HisabDao) {

    // Profile
    val profile: Flow<ProfileEntity?> = dao.getProfileFlow()
    suspend fun updateProfile(profile: ProfileEntity) = dao.insertProfile(profile)
    suspend fun getProfileDirect(): ProfileEntity? = dao.getProfile()

    // Accounts
    val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    suspend fun saveAccount(account: AccountEntity) = dao.insertAccount(account)
    suspend fun deleteAccount(id: String) = dao.deleteAccount(id)
    suspend fun adjustBalance(accountId: String, amount: Double) = dao.adjustAccountBalance(accountId, amount)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    suspend fun saveCategory(category: CategoryEntity) = dao.insertCategory(category)
    suspend fun deleteCategory(id: String) = dao.deleteCategory(id)

    // Shops
    val allShops: Flow<List<ShopEntity>> = dao.getAllShops()
    suspend fun saveShop(shop: ShopEntity) = dao.insertShop(shop)
    suspend fun deleteShop(id: String) = dao.deleteShop(id)

    // Transactions
    val allTransactionsWithItems: Flow<List<TransactionWithItems>> = dao.getAllTransactionsWithItems()
    fun getTransactionsByType(type: String): Flow<List<TransactionWithItems>> = dao.getTransactionsByType(type)

    suspend fun saveFullTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        // 1. If shop is named, resolve or create shop
        var effectiveShopId = transaction.shopId
        if (!transaction.shopName.isNullOrBlank()) {
            val normalized = transaction.shopName.trim().lowercase()
            val existingShop = dao.getShopByNormalizedName(normalized)
            if (existingShop != null) {
                effectiveShopId = existingShop.id
                dao.recordShopVisit(existingShop.id, transaction.totalAmount)
            } else {
                val newShop = ShopEntity(
                    id = UUID.randomUUID().toString(),
                    name = transaction.shopName.trim(),
                    normalizedName = normalized,
                    visitCount = 1,
                    totalSpent = transaction.totalAmount
                )
                dao.insertShop(newShop)
                effectiveShopId = newShop.id
            }
        }

        val updatedTransaction = transaction.copy(shopId = effectiveShopId)
        dao.insertTransaction(updatedTransaction)

        // 2. Insert line items
        if (items.isNotEmpty()) {
            val assignedItems = items.map { item ->
                item.copy(
                    transactionId = updatedTransaction.id,
                    normalizedItemName = item.itemName.trim().lowercase()
                )
            }
            dao.insertTransactionItems(assignedItems)
        }

        // 3. Adjust account balances
        if (!updatedTransaction.accountId.isNullOrBlank()) {
            when (updatedTransaction.transactionType) {
                TransactionType.EXPENSE.name -> {
                    dao.adjustAccountBalance(updatedTransaction.accountId, -updatedTransaction.totalAmount)
                }
                TransactionType.INCOME.name -> {
                    dao.adjustAccountBalance(updatedTransaction.accountId, updatedTransaction.totalAmount)
                }
                TransactionType.TRANSFER.name -> {
                    dao.adjustAccountBalance(updatedTransaction.accountId, -updatedTransaction.totalAmount)
                    if (!updatedTransaction.destinationAccountId.isNullOrBlank()) {
                        dao.adjustAccountBalance(updatedTransaction.destinationAccountId, updatedTransaction.totalAmount)
                    }
                }
            }
        }
    }

    suspend fun deleteTransaction(id: String) {
        val existing = dao.getTransactionWithItems(id)
        if (existing != null) {
            val tx = existing.transaction
            // Revert balance
            if (!tx.accountId.isNullOrBlank()) {
                when (tx.transactionType) {
                    TransactionType.EXPENSE.name -> dao.adjustAccountBalance(tx.accountId, tx.totalAmount)
                    TransactionType.INCOME.name -> dao.adjustAccountBalance(tx.accountId, -tx.totalAmount)
                    TransactionType.TRANSFER.name -> {
                        dao.adjustAccountBalance(tx.accountId, tx.totalAmount)
                        if (!tx.destinationAccountId.isNullOrBlank()) {
                            dao.adjustAccountBalance(tx.destinationAccountId, -tx.totalAmount)
                        }
                    }
                }
            }
            dao.deleteTransactionItems(id)
            dao.deleteTransaction(id)
        }
    }

    suspend fun updateFullTransaction(
        updatedTx: TransactionEntity,
        newItems: List<TransactionItemEntity>
    ) {
        val old = dao.getTransactionWithItems(updatedTx.id)
        if (old != null) {
            val oldTx = old.transaction
            // Revert old balance adjustment
            if (!oldTx.accountId.isNullOrBlank()) {
                when (oldTx.transactionType) {
                    TransactionType.EXPENSE.name -> dao.adjustAccountBalance(oldTx.accountId, oldTx.totalAmount)
                    TransactionType.INCOME.name -> dao.adjustAccountBalance(oldTx.accountId, -oldTx.totalAmount)
                    TransactionType.TRANSFER.name -> {
                        dao.adjustAccountBalance(oldTx.accountId, oldTx.totalAmount)
                        if (!oldTx.destinationAccountId.isNullOrBlank()) {
                            dao.adjustAccountBalance(oldTx.destinationAccountId, -oldTx.totalAmount)
                        }
                    }
                }
            }
        }

        // Save updated
        dao.insertTransaction(updatedTx)
        dao.deleteTransactionItems(updatedTx.id)
        if (newItems.isNotEmpty()) {
            val assigned = newItems.map { it.copy(transactionId = updatedTx.id, normalizedItemName = it.itemName.trim().lowercase()) }
            dao.insertTransactionItems(assigned)
        }

        // Apply new balance adjustment
        if (!updatedTx.accountId.isNullOrBlank()) {
            when (updatedTx.transactionType) {
                TransactionType.EXPENSE.name -> dao.adjustAccountBalance(updatedTx.accountId, -updatedTx.totalAmount)
                TransactionType.INCOME.name -> dao.adjustAccountBalance(updatedTx.accountId, updatedTx.totalAmount)
                TransactionType.TRANSFER.name -> {
                    dao.adjustAccountBalance(updatedTx.accountId, -updatedTx.totalAmount)
                    if (!updatedTx.destinationAccountId.isNullOrBlank()) {
                        dao.adjustAccountBalance(updatedTx.destinationAccountId, updatedTx.totalAmount)
                    }
                }
            }
        }
    }

    // Items
    val allItems: Flow<List<TransactionItemEntity>> = dao.getAllTransactionItems()
    suspend fun mergeItems(oldNormalized: String, newNormalized: String, newName: String) {
        dao.mergeItemNames(oldNormalized, newNormalized, newName)
    }

    suspend fun deleteItemsByNormalizedName(normalizedName: String) {
        dao.deleteItemsByNormalizedName(normalizedName)
    }

    suspend fun clearAllFinancialData() {
        dao.clearAllTransactions()
        dao.clearAllTransactionItems()
        dao.clearAllShops()
        dao.clearAllBudgets()
        dao.clearAllReceipts()
        dao.resetAccountBalances()
        dao.clearAiMessages()
    }

    // Budgets
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> = dao.getBudgetsForMonth(monthYear)
    suspend fun saveBudget(budget: BudgetEntity) = dao.insertBudget(budget)
    suspend fun deleteBudget(id: String) = dao.deleteBudget(id)

    // Receipts
    val allReceipts: Flow<List<ReceiptEntity>> = dao.getAllReceipts()
    suspend fun saveReceipt(receipt: ReceiptEntity) = dao.insertReceipt(receipt)
    suspend fun deleteReceipt(id: String) = dao.deleteReceipt(id)

    // API Keys
    val allApiKeys: Flow<List<AiApiKeyEntity>> = dao.getAllApiKeys()
    suspend fun getActiveApiKey(): AiApiKeyEntity? = dao.getActiveApiKey()
    suspend fun saveApiKey(key: AiApiKeyEntity) = dao.insertApiKey(key)
    suspend fun updateApiKey(key: AiApiKeyEntity) = dao.updateApiKey(key)
    suspend fun deleteApiKey(id: String) = dao.deleteApiKey(id)

    // AI Messages
    val allAiMessages: Flow<List<AiMessageEntity>> = dao.getAllAiMessages()
    suspend fun saveAiMessage(msg: AiMessageEntity) = dao.insertAiMessage(msg)
    suspend fun updateAiMessage(msg: AiMessageEntity) = dao.updateAiMessage(msg)
    suspend fun clearAiMessages() = dao.clearAiMessages()
    suspend fun deleteAiMessage(id: String) = dao.deleteAiMessage(id)
}
