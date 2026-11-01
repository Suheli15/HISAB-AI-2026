package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProfileEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        ShopEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        BudgetEntity::class,
        ReceiptEntity::class,
        AiApiKeyEntity::class,
        AiMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HisabDatabase : RoomDatabase() {

    abstract fun hisabDao(): HisabDao

    companion object {
        @Volatile
        private var INSTANCE: HisabDatabase? = null

        fun getDatabase(context: Context): HisabDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HisabDatabase::class.java,
                    "hisab_ai_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default seeds on background coroutine
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    seedDefaults(database.hisabDao())
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDefaults(dao: HisabDao) {
            // Profile
            dao.insertProfile(
                ProfileEntity(
                    id = "user_default",
                    name = "Nahid Hossain",
                    email = "teama04.nahidsworld@gmail.com",
                    currency = "BDT",
                    currencySymbol = "৳",
                    language = "bn",
                    monthlyBudget = 35000.0
                )
            )

            // Clean initial accounts (Zero balance - no fake demo data)
            val defaultAccounts = listOf(
                AccountEntity(id = "acc_cash", name = "Cash / নগদ", type = "CASH", balance = 0.0, isDefault = true),
                AccountEntity(id = "acc_bkash", name = "bKash", type = "BKASH", balance = 0.0),
                AccountEntity(id = "acc_nagad", name = "Nagad", type = "NAGAD", balance = 0.0),
                AccountEntity(id = "acc_bank", name = "Bank Account", type = "BANK", balance = 0.0)
            )
            for (acc in defaultAccounts) {
                dao.insertAccount(acc)
            }

            // Default Categories
            val defaultCategories = listOf(
                // Expenses
                CategoryEntity(id = "cat_groceries", name = "Groceries", nameBn = "বাজার ও সদাই", type = "EXPENSE", icon = "shopping_basket", colorHex = "#10B981"),
                CategoryEntity(id = "cat_food", name = "Food & Dining", nameBn = "খাবার ও রেস্টুরেন্ট", type = "EXPENSE", icon = "restaurant", colorHex = "#F59E0B"),
                CategoryEntity(id = "cat_transport", name = "Transport", nameBn = "যাতায়াত ও ভাড়া", type = "EXPENSE", icon = "directions_bus", colorHex = "#3B82F6"),
                CategoryEntity(id = "cat_utilities", name = "Utilities & Bills", nameBn = "বিল ও ইউটিলিটি", type = "EXPENSE", icon = "bolt", colorHex = "#8B5CF6"),
                CategoryEntity(id = "cat_shopping", name = "Shopping", nameBn = "কেনাকাটা", type = "EXPENSE", icon = "shopping_bag", colorHex = "#EC4899"),
                CategoryEntity(id = "cat_medical", name = "Medical & Health", nameBn = "চিকিৎসা ও ওষুধ", type = "EXPENSE", icon = "medical_services", colorHex = "#EF4444"),
                CategoryEntity(id = "cat_rent", name = "House Rent", nameBn = "বাড়ি ভাড়া", type = "EXPENSE", icon = "home", colorHex = "#6366F1"),
                CategoryEntity(id = "cat_education", name = "Education", nameBn = "শিক্ষা", type = "EXPENSE", icon = "school", colorHex = "#14B8A6"),
                CategoryEntity(id = "cat_entertainment", name = "Entertainment", nameBn = "বিনোদন", type = "EXPENSE", icon = "movie", colorHex = "#F97316"),
                CategoryEntity(id = "cat_other_exp", name = "Other Expense", nameBn = "অন্যান্য খরচ", type = "EXPENSE", icon = "more_horiz", colorHex = "#64748B"),

                // Incomes
                CategoryEntity(id = "cat_salary", name = "Salary", nameBn = "মাসিক বেতন", type = "INCOME", icon = "payments", colorHex = "#059669"),
                CategoryEntity(id = "cat_business", name = "Business Income", nameBn = "ব্যবসার আয়", type = "INCOME", icon = "storefront", colorHex = "#0D9488"),
                CategoryEntity(id = "cat_freelance", name = "Freelance", nameBn = "ফ্রিল্যান্সিং", type = "INCOME", icon = "laptop_mac", colorHex = "#0284C7"),
                CategoryEntity(id = "cat_investment", name = "Investment / Profit", nameBn = "বিনিয়োগ ও লাভ", type = "INCOME", icon = "trending_up", colorHex = "#16A34A"),
                CategoryEntity(id = "cat_other_inc", name = "Other Income", nameBn = "অন্যান্য আয়", type = "INCOME", icon = "account_balance_wallet", colorHex = "#059669")
            )
            dao.insertCategories(defaultCategories)

            // Seed initial AI API Key record for Gemini
            dao.insertApiKey(
                AiApiKeyEntity(
                    id = "key_gemini_default",
                    provider = "GEMINI",
                    apiKey = "",
                    model = "gemini-3.8-flash",
                    isEnabled = true,
                    priority = 1
                )
            )

            // Seed initial AI welcome message
            dao.insertAiMessage(
                AiMessageEntity(
                    id = "welcome_msg",
                    role = "assistant",
                    content = "আসসালামু আলাইকুম! আমি হিসাব এআই (Hisab AI)। আপনার ব্যক্তিগত ফাইন্যান্স সহকারী। আপনি বাংলা বা ইংরেজিতে খরচ বা আয়ের কথা লিখে পাঠাতে পারেন। যেমন:\n\n• \"আজকে মিনা বাজার থেকে ১ কেজি চাল ১৪০ টাকা, ১ লিটার তেল ১৮০ টাকা, ৫০০ টাকার মাছ কিনেছি।\"\n• \"গতকাল বেতন পেয়েছি ২৫,০০০ টাকা।\"\n• \"I spent 350 taka on transport today.\"",
                    language = "bn"
                )
            )
        }
    }
}
