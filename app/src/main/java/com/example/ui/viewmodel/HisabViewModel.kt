package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiApiKeyEntity
import com.example.ai.AiExtractionResult
import com.example.ai.FinancialAssistantEngine
import com.example.ai.FirebaseAiService
import com.example.ai.GeminiAiService
import com.example.ai.ParsedItem
import com.example.ai.ParsedTransaction
import com.example.data.local.HisabDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AiMessageEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.ReceiptEntity
import com.example.data.model.ShopEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithItems
import com.example.data.repository.HisabRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class DateFilterType {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL
}

class HisabViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HisabRepository

    init {
        val db = HisabDatabase.getDatabase(application)
        repository = HisabRepository(db.hisabDao())
    }

    // Flows from repository
    val profile: StateFlow<ProfileEntity?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shops: StateFlow<List<ShopEntity>> = repository.allShops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionWithItems>> = repository.allTransactionsWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<TransactionItemEntity>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receipts: StateFlow<List<ReceiptEntity>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiKeys: StateFlow<List<AiApiKeyEntity>> = repository.allApiKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiMessages: StateFlow<List<AiMessageEntity>> = repository.allAiMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _selectedDateFilter = MutableStateFlow(DateFilterType.THIS_MONTH)
    val selectedDateFilter = _selectedDateFilter.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping = _isAiTyping.asStateFlow()

    private val _pendingTransaction = MutableStateFlow<ParsedTransaction?>(null)
    val pendingTransaction = _pendingTransaction.asStateFlow()

    private val _uiToastMessage = MutableStateFlow<String?>(null)
    val uiToastMessage = _uiToastMessage.asStateFlow()

    // Assistant State
    private val _assistantChat = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            Pair("assistant", "আসসালামু আলাইকুম! আপনার আর্থিক ডাটাবেজ থেকে যেকোনো প্রশ্ন করুন। যেমন:\n• \"এই মাসে মাছ কিনতে কত খরচ করেছি?\"\n• \"আমার বাজেট কত বাকি?\"\n• \"সবচেয়ে বেশি খরচ কোন দোকানে?\"\n• \"আজকের মোট খরচ কত?\"")
        )
    )
    val assistantChat = _assistantChat.asStateFlow()

    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking = _isAssistantThinking.asStateFlow()

    // Receipt scanner state
    private val _scannedReceipt = MutableStateFlow<ReceiptEntity?>(null)
    val scannedReceipt = _scannedReceipt.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    fun setDateFilter(filter: DateFilterType) {
        _selectedDateFilter.value = filter
    }

    fun clearToast() {
        _uiToastMessage.value = null
    }

    // --- AI Expense Chat Logic ---
    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val cleanText = userText.trim()

        viewModelScope.launch(Dispatchers.IO) {
            // Save user message to database
            repository.saveAiMessage(
                AiMessageEntity(
                    role = "user",
                    content = cleanText,
                    timestamp = System.currentTimeMillis()
                )
            )

            _isAiTyping.value = true

            val activeKey = repository.getActiveApiKey()
            val (result, tokens) = if (activeKey != null && activeKey.apiKey.isNotBlank()) {
                GeminiAiService.parseWithGeminiOrFallback(
                    rawMessage = cleanText,
                    apiKey = activeKey.apiKey,
                    model = activeKey.model,
                    pendingTransaction = _pendingTransaction.value
                )
            } else {
                FirebaseAiService.parseWithGemini(
                    rawMessage = cleanText,
                    pendingTransaction = _pendingTransaction.value
                )
            }

            // Update token usage if key exists
            if (activeKey != null && tokens > 0) {
                repository.updateApiKey(
                    activeKey.copy(
                        requestCount = activeKey.requestCount + 1,
                        tokenUsage = activeKey.tokenUsage + tokens
                    )
                )
            }

            _isAiTyping.value = false

            if (result.transaction != null) {
                _pendingTransaction.value = result.transaction
            }

            // Save assistant response message to DB
            repository.saveAiMessage(
                AiMessageEntity(
                    role = "assistant",
                    content = result.replyMessage,
                    pendingTransactionJson = result.transaction?.let { "${it.type}|${it.totalAmount}" },
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun confirmPendingTransaction(customShopName: String? = null, customPayment: String? = null) {
        val pending = _pendingTransaction.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val finalShop = customShopName ?: pending.shopName
            val finalPayment = customPayment ?: pending.paymentMethod

            val defaultAcc = accounts.value.firstOrNull { it.isDefault } ?: accounts.value.firstOrNull()

            val txDateMillis = try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.parse(pending.date)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            val txEntity = TransactionEntity(
                id = pending.id,
                transactionType = pending.type,
                transactionDate = txDateMillis,
                dateString = pending.date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                shopName = finalShop,
                accountId = defaultAcc?.id,
                categoryName = pending.categoryName,
                totalAmount = pending.totalAmount,
                currency = pending.currency,
                paymentMethod = finalPayment,
                notes = pending.notes,
                source = TransactionSource.CHAT_AI.name
            )

            val itemEntities = pending.items.map { item ->
                TransactionItemEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = pending.id,
                    itemName = item.itemName,
                    normalizedItemName = item.itemName.trim().lowercase(),
                    quantity = item.quantity,
                    unit = item.unit,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    categoryName = item.category
                )
            }

            repository.saveFullTransaction(txEntity, itemEntities)

            // Confirmation message in chat
            repository.saveAiMessage(
                AiMessageEntity(
                    role = "assistant",
                    content = "✅ লেনদেনটি সফলভাবে ডাটাবেজে সংরক্ষণ করা হয়েছে! (৳ ${String.format(Locale.US, "%,.0f", pending.totalAmount)})",
                    isConfirmed = true,
                    timestamp = System.currentTimeMillis()
                )
            )

            _pendingTransaction.value = null
            _uiToastMessage.value = "লেনদেন সংরক্ষিত হয়েছে"
        }
    }

    fun cancelPendingTransaction() {
        _pendingTransaction.value = null
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAiMessage(
                AiMessageEntity(
                    role = "assistant",
                    content = "লেনদেনটি বাতিল করা হয়েছে।",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun updatePendingItemPrice(index: Int, newPrice: Double) {
        val curr = _pendingTransaction.value ?: return
        val updatedItems = curr.items.toMutableList()
        if (index in updatedItems.indices) {
            val old = updatedItems[index]
            updatedItems[index] = old.copy(
                totalPrice = newPrice,
                unitPrice = if (old.quantity > 0) newPrice / old.quantity else newPrice
            )
            val newTotal = updatedItems.sumOf { it.totalPrice }
            _pendingTransaction.value = curr.copy(
                items = updatedItems,
                totalAmount = newTotal
            )
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAiMessages()
            _pendingTransaction.value = null
            // Re-seed welcome message
            repository.saveAiMessage(
                AiMessageEntity(
                    role = "assistant",
                    content = "আসসালামু আলাইকুম! নতুন লেনদেন যোগ করতে যেকোনো খরচের বিবরণ বাংলায় বা ইংরেজিতে লিখুন।",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Financial Assistant Q&A ---
    fun askAssistant(queryText: String) {
        if (queryText.isBlank()) return
        val currentList = _assistantChat.value.toMutableList()
        currentList.add(Pair("user", queryText))
        _assistantChat.value = currentList
        _isAssistantThinking.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val budget = profile.value?.monthlyBudget ?: 35000.0
            val currency = profile.value?.currencySymbol ?: "৳"
            val answer = FinancialAssistantEngine.answerQuery(
                query = queryText,
                transactions = transactions.value,
                monthlyBudget = budget,
                currencySymbol = currency
            )

            val updated = _assistantChat.value.toMutableList()
            updated.add(Pair("assistant", answer))
            _assistantChat.value = updated
            _isAssistantThinking.value = false
        }
    }

    // --- Receipt Scanner ---
    fun simulateReceiptScan(imageUri: Uri?) {
        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(1200) // realistic OCR processing delay
            val randomTotal = 1450.0
            val receipt = ReceiptEntity(
                id = UUID.randomUUID().toString(),
                imageUri = imageUri?.toString() ?: "",
                merchantName = "Meena Bazar / মিনা বাজার",
                totalAmount = randomTotal,
                dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                rawOcrText = "MEENA BAZAR\nDATE: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}\n1. RICE 5KG - 650 TK\n2. SOYBEAN OIL 2L - 380 TK\n3. EGGS 1 DOZ - 140 TK\n4. SPICES - 280 TK\nTOTAL: 1450 TK\nTHANK YOU FOR SHOPPING",
                isProcessed = true
            )
            _scannedReceipt.value = receipt
            repository.saveReceipt(receipt)
            _isScanning.value = false
            _uiToastMessage.value = "রসিদ স্ক্যান সফল হয়েছে!"
        }
    }

    fun saveReceiptAsTransaction(receipt: ReceiptEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultAcc = accounts.value.firstOrNull { it.isDefault } ?: accounts.value.firstOrNull()
            val txEntity = TransactionEntity(
                id = UUID.randomUUID().toString(),
                transactionType = TransactionType.EXPENSE.name,
                transactionDate = System.currentTimeMillis(),
                dateString = receipt.dateString.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
                shopName = receipt.merchantName,
                accountId = defaultAcc?.id,
                categoryName = "Groceries",
                totalAmount = receipt.totalAmount,
                currency = "BDT",
                paymentMethod = "Cash",
                notes = "রসিদ স্ক্যান থেকে যুক্ত (Receipt #${receipt.id.take(6)})",
                source = TransactionSource.RECEIPT_SCANNER.name
            )

            val items = listOf(
                TransactionItemEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = txEntity.id,
                    itemName = "চাল / Rice 5kg",
                    normalizedItemName = "চাল",
                    quantity = 5.0,
                    unit = "kg",
                    unitPrice = 130.0,
                    totalPrice = 650.0,
                    categoryName = "Groceries"
                ),
                TransactionItemEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = txEntity.id,
                    itemName = "সয়াবিন তেল / Oil 2L",
                    normalizedItemName = "তেল",
                    quantity = 2.0,
                    unit = "liter",
                    unitPrice = 190.0,
                    totalPrice = 380.0,
                    categoryName = "Groceries"
                ),
                TransactionItemEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = txEntity.id,
                    itemName = "ডিম / Eggs (1 Dozen)",
                    normalizedItemName = "ডিম",
                    quantity = 12.0,
                    unit = "pcs",
                    unitPrice = 11.66,
                    totalPrice = 140.0,
                    categoryName = "Groceries"
                ),
                TransactionItemEntity(
                    id = UUID.randomUUID().toString(),
                    transactionId = txEntity.id,
                    itemName = "মসলা ও টুকিটাকি",
                    normalizedItemName = "মসলা",
                    quantity = 1.0,
                    unit = "packet",
                    unitPrice = 280.0,
                    totalPrice = 280.0,
                    categoryName = "Groceries"
                )
            )

            repository.saveFullTransaction(txEntity, items)
            _scannedReceipt.value = null
            _uiToastMessage.value = "রসিদের লেনদেন সংরক্ষিত হয়েছে!"
        }
    }

    fun clearScannedReceipt() {
        _scannedReceipt.value = null
    }

    // --- CRUD Operations ---
    fun deleteTransaction(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(id)
            _uiToastMessage.value = "লেনদেন মুছে ফেলা হয়েছে"
        }
    }

    fun saveManualTransaction(
        type: String,
        amount: Double,
        category: String,
        account: AccountEntity?,
        shopName: String,
        notes: String,
        dateString: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val txDateMillis = try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            val tx = TransactionEntity(
                id = UUID.randomUUID().toString(),
                transactionType = type,
                transactionDate = txDateMillis,
                dateString = dateString,
                shopName = shopName.takeIf { it.isNotBlank() },
                accountId = account?.id,
                categoryName = category,
                totalAmount = amount,
                currency = "BDT",
                paymentMethod = account?.name ?: "Cash",
                notes = notes,
                source = TransactionSource.MANUAL.name
            )

            val item = TransactionItemEntity(
                id = UUID.randomUUID().toString(),
                transactionId = tx.id,
                itemName = notes.ifBlank { category },
                normalizedItemName = (notes.ifBlank { category }).trim().lowercase(),
                quantity = 1.0,
                unit = "pcs",
                unitPrice = amount,
                totalPrice = amount,
                categoryName = category
            )

            repository.saveFullTransaction(tx, listOf(item))
            _uiToastMessage.value = "লেনদেন সংরক্ষিত হয়েছে"
        }
    }

    fun saveBudget(categoryId: String, categoryName: String, limit: Double, monthYear: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val budget = BudgetEntity(
                categoryId = categoryId,
                categoryName = categoryName,
                monthYear = monthYear,
                limitAmount = limit
            )
            repository.saveBudget(budget)
            _uiToastMessage.value = "বাজেট আপডেট করা হয়েছে"
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBudget(id)
            _uiToastMessage.value = "বাজেট মুছে ফেলা হয়েছে"
        }
    }

    fun saveAccount(name: String, type: String, initialBalance: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = AccountEntity(
                name = name,
                type = type,
                balance = initialBalance
            )
            repository.saveAccount(account)
            _uiToastMessage.value = "একাউন্ট যুক্ত হয়েছে"
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAccount(id)
            _uiToastMessage.value = "একাউন্ট মুছে ফেলা হয়েছে"
        }
    }

    fun saveShop(name: String, address: String, phone: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val shop = ShopEntity(
                name = name,
                normalizedName = name.trim().lowercase(),
                address = address,
                phone = phone
            )
            repository.saveShop(shop)
            _uiToastMessage.value = "দোকান সংরক্ষিত হয়েছে"
        }
    }

    fun deleteShop(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteShop(id)
            _uiToastMessage.value = "দোকান মুছে ফেলা হয়েছে"
        }
    }

    fun mergeItem(oldNormalized: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.mergeItems(oldNormalized, newName.trim().lowercase(), newName)
            _uiToastMessage.value = "আইটেম মার্জ সম্পন্ন হয়েছে"
        }
    }

    fun saveApiKey(provider: String, apiKey: String, model: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = apiKeys.value.firstOrNull { it.provider == provider }
            val entity = (existing ?: AiApiKeyEntity(provider = provider, apiKey = apiKey, model = model)).copy(
                apiKey = apiKey,
                model = model,
                isEnabled = true
            )
            repository.saveApiKey(entity)
            _uiToastMessage.value = "$provider API কি সংরক্ষিত হয়েছে"
        }
    }

    fun toggleApiKey(key: AiApiKeyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateApiKey(key.copy(isEnabled = !key.isEnabled))
        }
    }

    fun testApiKey(key: AiApiKeyEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSuccess = GeminiAiService.testConnection(key.apiKey, key.model)
            repository.updateApiKey(
                key.copy(
                    isConnected = isSuccess,
                    lastTestedAt = System.currentTimeMillis(),
                    lastError = if (isSuccess) null else "সংযোগ ব্যর্থ হয়েছে। API কি বা ইন্টারনেট চেক করুন।"
                )
            )
            _uiToastMessage.value = if (isSuccess) "✅ সংযোগ সফল!" else "❌ সংযোগ ব্যর্থ হয়েছে!"
        }
    }

    fun deleteApiKey(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteApiKey(id)
            _uiToastMessage.value = "API কি মুছে ফেলা হয়েছে"
        }
    }

    fun updateProfile(name: String, currency: String, symbol: String, language: String, monthlyBudget: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = profile.value ?: ProfileEntity()
            repository.updateProfile(
                current.copy(
                    name = name,
                    currency = currency,
                    currencySymbol = symbol,
                    language = language,
                    monthlyBudget = monthlyBudget
                )
            )
            _uiToastMessage.value = "প্রোফাইল আপডেট হয়েছে"
        }
    }

    fun resetDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            val db = HisabDatabase.getDatabase(getApplication())
            HisabDatabase.seedDefaults(db.hisabDao())
            _uiToastMessage.value = "ডিফল্ট ডাটা রিসেট হয়েছে"
        }
    }
}
