package com.example.ai

import com.example.data.model.TransactionWithItems
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FinancialAssistantEngine {

    fun answerQuery(
        query: String,
        transactions: List<TransactionWithItems>,
        monthlyBudget: Double,
        currencySymbol: String = "৳"
    ): String {
        val lower = query.lowercase(Locale.getDefault())

        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val cal = Calendar.getInstance()
        val thisMonthExpenses = transactions.filter { tx ->
            cal.timeInMillis = tx.transaction.transactionDate
            cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear &&
                    tx.transaction.transactionType == "EXPENSE"
        }

        val totalExpenseThisMonth = thisMonthExpenses.sumOf { it.transaction.totalAmount }
        val thisMonthIncomes = transactions.filter { tx ->
            cal.timeInMillis = tx.transaction.transactionDate
            cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear &&
                    tx.transaction.transactionType == "INCOME"
        }
        val totalIncomeThisMonth = thisMonthIncomes.sumOf { it.transaction.totalAmount }

        // Category breakdown this month
        val categoryTotals = mutableMapOf<String, Double>()
        for (tx in thisMonthExpenses) {
            val cat = tx.transaction.categoryName ?: "Other"
            categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + tx.transaction.totalAmount
        }

        // Shop breakdown
        val shopTotals = mutableMapOf<String, Double>()
        for (tx in thisMonthExpenses) {
            val shop = tx.transaction.shopName?.takeIf { it.isNotBlank() } ?: "Local Market / সাধারণ দোকান"
            shopTotals[shop] = (shopTotals[shop] ?: 0.0) + tx.transaction.totalAmount
        }

        // Item breakdown
        val itemTotals = mutableMapOf<String, Double>()
        for (tx in thisMonthExpenses) {
            for (item in tx.items) {
                val name = item.itemName
                itemTotals[name] = (itemTotals[name] ?: 0.0) + item.totalPrice
            }
        }

        return when {
            // Budget remaining / status
            lower.contains("বাজেট") || lower.contains("budget") || lower.contains("বাকি") -> {
                val remaining = monthlyBudget - totalExpenseThisMonth
                val percentage = if (monthlyBudget > 0) (totalExpenseThisMonth / monthlyBudget * 100).toInt() else 0
                val grocerySpent = categoryTotals["Groceries"] ?: 0.0
                val groceryPercent = if (monthlyBudget > 0) (grocerySpent / monthlyBudget * 100).toInt() else 0

                if (lower.contains("বাজার") || lower.contains("grocer")) {
                    "এই মাসে আপনার মাসিক বাজেট $currencySymbol${format(monthlyBudget)}-এর মধ্যে বাজার খরচে ব্যয় হয়েছে $currencySymbol${format(grocerySpent)} (বাজেটের প্রায় $groceryPercent%)।"
                } else {
                    "চলতি মাসে আপনার নির্ধারিত বাজেট: $currencySymbol${format(monthlyBudget)}\n" +
                            "মোট খরচ: $currencySymbol${format(totalExpenseThisMonth)} ($percentage% ব্যবহৃত)\n" +
                            "বাজেট অবশিষ্ট রয়েছে: $currencySymbol${format(remaining)}।" +
                            if (remaining < 0) "\n⚠️ সতর্কতা: আপনি বাজেট অতিক্রম করেছেন!" else ""
                }
            }

            // Grocery / specific category expenses
            lower.contains("মাছ") || lower.contains("fish") -> {
                val fishSpent = itemTotals.entries
                    .filter { it.key.contains("মাছ") || it.key.contains("fish", true) }
                    .sumOf { it.value }
                "এই মাসে আপনি মাছ কেনায় মোট ব্যয় করেছেন $currencySymbol${format(fishSpent)}।"
            }

            lower.contains("বাজার") || lower.contains("groceries") || lower.contains("চাল") || lower.contains("তেল") -> {
                val grocerySpent = categoryTotals["Groceries"] ?: 0.0
                "চলতি মাসে মুদি ও বাজার সদাই খাতে আপনার মোট খরচ হয়েছে $currencySymbol${format(grocerySpent)}।"
            }

            lower.contains("যাতায়াত") || lower.contains("ভাড়া") || lower.contains("transport") -> {
                val transportSpent = categoryTotals["Transport"] ?: 0.0
                "চলতি মাসে যাতায়াত খাতে মোট খরচ হয়েছে $currencySymbol${format(transportSpent)}।"
            }

            // Most spent shop / store
            lower.contains("দোকান") || lower.contains("shop") || lower.contains("merchant") -> {
                val topShop = shopTotals.maxByOrNull { it.value }
                if (topShop != null) {
                    "চলতি মাসে সবচেয়ে বেশি খরচ হয়েছে \"${topShop.key}\"-এ, মোট $currencySymbol${format(topShop.value)}।"
                } else {
                    "এই মাসে এখনো কোনো নির্দিষ্ট দোকানের হিসাব রেকর্ড করা হয়নি।"
                }
            }

            // Highest category
            lower.contains("সবচেয়ে বেশি") || lower.contains("highest") || lower.contains("শীর্ষ") -> {
                val topCategory = categoryTotals.maxByOrNull { it.value }
                if (topCategory != null) {
                    val percent = if (totalExpenseThisMonth > 0) (topCategory.value / totalExpenseThisMonth * 100).toInt() else 0
                    "এই মাসে আপনার সর্বোচ্চ ব্যয়ের খাত হলো \"${topCategory.key}\", যেখানে $currencySymbol${format(topCategory.value)} খরচ হয়েছে (মোট খরচের $percent%)।"
                } else {
                    "এই মাসে এখনো কোনো খরচের তথ্য নেই।"
                }
            }

            // Total expense this month / daily average
            lower.contains("খরচ") || lower.contains("spend") || lower.contains("expense") -> {
                val daysPassed = maxOf(now.get(Calendar.DAY_OF_MONTH), 1)
                val dailyAvg = totalExpenseThisMonth / daysPassed
                "চলতি মাসে আপনার মোট খরচ: $currencySymbol${format(totalExpenseThisMonth)}\n" +
                        "চলতি মাসে দৈনিক গড় খরচ: $currencySymbol${format(dailyAvg)}\n" +
                        "মোট খরচের সংখ্যা: ${thisMonthExpenses.size} টি লেনদেন।"
            }

            // Savings / Net Cash Flow
            lower.contains("সঞ্চয়") || lower.contains("save") || lower.contains("saving") || lower.contains("ব্যালেন্স") -> {
                val savings = totalIncomeThisMonth - totalExpenseThisMonth
                "চলতি মাসে মোট আয়: $currencySymbol${format(totalIncomeThisMonth)}\n" +
                        "মোট ব্যয়: $currencySymbol${format(totalExpenseThisMonth)}\n" +
                        "চলতি মাসে নিট সঞ্চয়: $currencySymbol${format(savings)} " +
                        if (savings >= 0) "✅ (পজিটিভ ক্যাশফ্লো)" else "⚠️ (নেগেটিভ ক্যাশফ্লো)"
            }

            // Default insightful summary
            else -> {
                val topCat = categoryTotals.maxByOrNull { it.value }
                val catText = topCat?.let { "শীর্ষ ব্যয়ের খাত: ${it.key} ($currencySymbol${format(it.value)})" } ?: ""
                "চলতি মাসের আর্থিক সারাংশ:\n" +
                        "• মোট খরচ: $currencySymbol${format(totalExpenseThisMonth)}\n" +
                        "• মোট আয়: $currencySymbol${format(totalIncomeThisMonth)}\n" +
                        (if (catText.isNotEmpty()) "• $catText\n" else "") +
                        "• অবশিষ্ট বাজেট: $currencySymbol${format(monthlyBudget - totalExpenseThisMonth)}"
            }
        }
    }

    private fun format(num: Double): String {
        return String.format(Locale.US, "%,.0f", num)
    }
}
