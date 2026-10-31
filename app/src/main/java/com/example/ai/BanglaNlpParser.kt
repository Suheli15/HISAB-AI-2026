package com.example.ai

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

object BanglaNlpParser {

    private val banglaToEnglishDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    fun convertBanglaDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            sb.append(banglaToEnglishDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    private fun getCurrentDateFormatted(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getYesterdayDateFormatted(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    /**
     * Checks if input is a contextual follow-up to an existing pending transaction
     */
    fun isContextualFollowUp(input: String, pending: ParsedTransaction?): Boolean {
        if (pending == null) return false
        val lower = input.lowercase().trim()
        val isShopFollowUp = lower.contains("দোকান") || lower.contains("থেকে") || lower.contains("from") || lower.contains("store") || lower.contains("shop") || lower.contains("এটা")
        val isPaymentFollowUp = lower.contains("নগদ") || lower.contains("বিকাশ") || lower.contains("bkash") || lower.contains("card") || lower.contains("cash") || lower.contains("bank")
        return isShopFollowUp || isPaymentFollowUp
    }

    fun applyFollowUp(input: String, pending: ParsedTransaction): ParsedTransaction {
        var updated = pending
        val text = convertBanglaDigits(input)

        // Extract shop if mentioned
        val shopRegex = Regex("""(?:থেকে|from|store|দোকান|হচ্ছে|ছিল)\s*[:=]?\s*([A-Za-z0-9\u0980-\u09FF\s]+)""", RegexOption.IGNORE_CASE)
        val shopMatch = shopRegex.find(text)
        if (shopMatch != null) {
            val cleanShop = shopMatch.groupValues[1].replace(Regex("""(থেকে|ছিল|দোকান|হচ্ছে|এটা)"""), "").trim()
            if (cleanShop.isNotBlank()) {
                updated = updated.copy(shopName = cleanShop)
            }
        } else if (input.contains("থেকে") || input.contains("from")) {
            val parts = input.split(Regex("""থেকে|from"""))
            val candidate = parts.firstOrNull()?.replace("এটা", "")?.trim()
            if (!candidate.isNullOrBlank()) {
                updated = updated.copy(shopName = candidate)
            }
        }

        // Extract payment method
        val lower = text.lowercase()
        when {
            lower.contains("নগদ") || lower.contains("cash") -> updated = updated.copy(paymentMethod = "Cash")
            lower.contains("বিকাশ") || lower.contains("bkash") -> updated = updated.copy(paymentMethod = "bKash")
            lower.contains("নগদ") || lower.contains("nagad") -> updated = updated.copy(paymentMethod = "Nagad")
            lower.contains("ব্যাংক") || lower.contains("bank") -> updated = updated.copy(paymentMethod = "Bank")
            lower.contains("কার্ড") || lower.contains("card") -> updated = updated.copy(paymentMethod = "Credit Card")
        }

        return updated
    }

    fun parseMessage(rawInput: String, pendingTransaction: ParsedTransaction? = null): AiExtractionResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return AiExtractionResult(
                intent = "unknown",
                replyMessage = "অনুগ্রহ করে আপনার খরচ বা আয়ের বিবরণ লিখুন।"
            )
        }

        // 1. Check if contextual follow-up to pending transaction
        if (pendingTransaction != null && isContextualFollowUp(trimmed, pendingTransaction)) {
            val updated = applyFollowUp(trimmed, pendingTransaction)
            val shopMsg = if (!updated.shopName.isNullOrBlank()) "দোকান: ${updated.shopName}" else ""
            val payMsg = "পেমেন্ট: ${updated.paymentMethod}"
            return AiExtractionResult(
                intent = "follow_up_update",
                language = "bn",
                requiresConfirmation = true,
                transaction = updated,
                replyMessage = "হিসাব আপডেট করা হয়েছে ($shopMsg $payMsg)। সংরক্ষণ করতে নিশ্চিত করুন।"
            )
        }

        val textWithEnglishDigits = convertBanglaDigits(trimmed)
        val lower = textWithEnglishDigits.lowercase()

        // 2. Check if Financial Inquiry / Query
        val isQuestion = lower.contains("?") || lower.contains("কত") || lower.contains("how much") ||
                lower.contains("তুলনা") || lower.contains("বাকি") || lower.contains("কোথায়") || lower.contains("which")
        if (isQuestion && !lower.contains("টাকা") && !lower.contains("কিনেছি") && !lower.contains("spent") && !lower.contains("পেয়েছি")) {
            return AiExtractionResult(
                intent = "query_finances",
                language = if (lower.contains("how") || lower.contains("what")) "en" else "bn",
                queryAnswer = null,
                replyMessage = "আপনার ডাটাবেজ থেকে তথ্য অনুসন্ধান করা হচ্ছে..."
            )
        }

        // 3. Check for Income vs Expense
        val isIncome = lower.contains("বেতন") || lower.contains("salary") || lower.contains("income") ||
                lower.contains("পেলাম") || lower.contains("পাইলাম") || lower.contains("পেয়েছি") ||
                lower.contains("received") || lower.contains("লাভ") || lower.contains("বোনাস")

        // Determine date
        var txDate = getCurrentDateFormatted()
        if (lower.contains("গতকাল") || lower.contains("yesterday")) {
            txDate = getYesterdayDateFormatted()
        }

        // Extract Shop Name
        var extractedShop: String? = null
        val shopPattern = Regex("""([A-Za-z0-9\u0980-\u09FF\s]+?)\s+(?:থেকে|from|store|মার্কেট|সুপারশপ|দোকান)""", RegexOption.IGNORE_CASE)
        val shopMatch = shopPattern.find(textWithEnglishDigits)
        if (shopMatch != null) {
            val sName = shopMatch.groupValues[1]
                .replace(Regex("""(আজকে|গতকাল|আজ|yesterday|today)""", RegexOption.IGNORE_CASE), "")
                .trim()
            if (sName.length > 1 && !sName.equals("টাকা", true) && !sName.equals("tk", true)) {
                extractedShop = sName
            }
        }

        // Extract Payment Method
        var paymentMethod = "Cash"
        when {
            lower.contains("বিকাশ") || lower.contains("bkash") -> paymentMethod = "bKash"
            lower.contains("নগদ অ্যাপ") || lower.contains("nagad") -> paymentMethod = "Nagad"
            lower.contains("ব্যাংক") || lower.contains("bank") -> paymentMethod = "Bank"
            lower.contains("কার্ড") || lower.contains("card") -> paymentMethod = "Credit Card"
        }

        // 4. Handle Income
        if (isIncome) {
            val amountRegex = Regex("""(\d+(?:,\d+)*(?:\.\d+)?)\s*(?:টাকা|tk|bdt|taka)?""", RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.findAll(textWithEnglishDigits)
                .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .firstOrNull { it > 0 } ?: 0.0

            val category = when {
                lower.contains("বেতন") || lower.contains("salary") -> "Salary"
                lower.contains("ফ্রিল্যান্সিং") || lower.contains("freelance") -> "Freelance"
                lower.contains("ব্যবসা") || lower.contains("business") -> "Business Income"
                else -> "Other Income"
            }

            val tx = ParsedTransaction(
                id = UUID.randomUUID().toString(),
                type = "INCOME",
                date = txDate,
                shopName = null,
                paymentMethod = paymentMethod,
                currency = "BDT",
                items = listOf(
                    ParsedItem(
                        itemName = if (category == "Salary") "মাসিক বেতন / Salary" else "আয় / Income",
                        quantity = 1.0,
                        unit = "মাস",
                        unitPrice = amountMatch,
                        totalPrice = amountMatch,
                        category = category
                    )
                ),
                totalAmount = amountMatch,
                notes = trimmed,
                categoryName = category
            )

            return AiExtractionResult(
                intent = "create_income",
                language = "bn",
                requiresConfirmation = true,
                transaction = tx,
                replyMessage = "আয়ের হিসাব পেয়েছি: ৳ ${formatNumber(amountMatch)} ($category)। অনুগ্রহ করে নিশ্চিত করুন।"
            )
        }

        // 5. Handle Expense with multiple items
        val parsedItems = mutableListOf<ParsedItem>()

        // Split by punctuation: comma, 'এবং', 'and', 'ও'
        val segments = textWithEnglishDigits.split(Regex("[,;\\n]+|\\s+এবং\\s+|\\s+and\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (seg in segments) {
            // Pattern A: quantity + unit + item + price ("১ কেজি চাল ১৪০ টাকা", "1 ltr oil 180 tk")
            val patternA = Regex("""(\d+(?:\.\d+)?)\s*(কেজি|kg|লিটার|liter|ltr|প্যাকেট|packet|পিস|pcs|ডজন|dozen|গ্রাম|gm)?\s+([A-Za-z\u0980-\u09FF\s]+?)\s+(\d+(?:\.\d+)?)\s*(?:টাকা|tk|bdt|taka)?""", RegexOption.IGNORE_CASE)
            val matchA = patternA.find(seg)

            if (matchA != null) {
                val qty = matchA.groupValues[1].toDoubleOrNull() ?: 1.0
                val unit = matchA.groupValues[2].ifBlank { "pcs" }
                val rawItem = matchA.groupValues[3].trim()
                val price = matchA.groupValues[4].toDoubleOrNull() ?: 0.0

                if (price > 0 && rawItem.isNotBlank()) {
                    val cleanItem = cleanItemName(rawItem)
                    parsedItems.add(
                        ParsedItem(
                            itemName = cleanItem,
                            quantity = qty,
                            unit = unit,
                            unitPrice = if (qty > 0) (price / qty) else price,
                            totalPrice = price,
                            category = detectCategory(cleanItem)
                        )
                    )
                    continue
                }
            }

            // Pattern B: price + item ("৫০০ টাকার মাছ", "350 taka on transport")
            val patternB = Regex("""(\d+(?:\.\d+)?)\s*(?:টাকা|টাকার|tk|taka|bdt)\s*(?:এর|on|for)?\s*([A-Za-z\u0980-\u09FF\s]+)""", RegexOption.IGNORE_CASE)
            val matchB = patternB.find(seg)
            if (matchB != null) {
                val price = matchB.groupValues[1].toDoubleOrNull() ?: 0.0
                val rawItem = matchB.groupValues[2].trim()
                if (price > 0 && rawItem.isNotBlank()) {
                    val cleanItem = cleanItemName(rawItem)
                    parsedItems.add(
                        ParsedItem(
                            itemName = cleanItem,
                            quantity = 1.0,
                            unit = "item",
                            unitPrice = price,
                            totalPrice = price,
                            category = detectCategory(cleanItem)
                        )
                    )
                    continue
                }
            }

            // Pattern C: item + price ("বাস ভাড়া ৫০ টাকা", "rice 140")
            val patternC = Regex("""([A-Za-z\u0980-\u09FF\s]+?)\s+(\d+(?:\.\d+)?)\s*(?:টাকা|tk|bdt|taka)""", RegexOption.IGNORE_CASE)
            val matchC = patternC.find(seg)
            if (matchC != null) {
                val rawItem = matchC.groupValues[1].trim()
                val price = matchC.groupValues[2].toDoubleOrNull() ?: 0.0
                if (price > 0 && rawItem.isNotBlank()) {
                    val cleanItem = cleanItemName(rawItem)
                    parsedItems.add(
                        ParsedItem(
                            itemName = cleanItem,
                            quantity = 1.0,
                            unit = "pcs",
                            unitPrice = price,
                            totalPrice = price,
                            category = detectCategory(cleanItem)
                        )
                    )
                    continue
                }
            }
        }

        // If no structured items found by segments, fallback to whole string search
        if (parsedItems.isEmpty()) {
            val amountRegex = Regex("""(\d+(?:,\d+)*(?:\.\d+)?)\s*(?:টাকা|টাকার|tk|bdt|taka)?""", RegexOption.IGNORE_CASE)
            val allAmounts = amountRegex.findAll(textWithEnglishDigits)
                .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .filter { it > 0 }
                .toList()

            val primaryAmount = allAmounts.maxOrNull() ?: 0.0
            if (primaryAmount > 0) {
                val itemName = cleanItemName(
                    trimmed.replace(Regex("""(\d+|টাকা|টাকার|tk|bdt|taka|আজকে|গতকাল|কিনেছি|spent|on)""", RegexOption.IGNORE_CASE), "")
                        .trim()
                ).ifBlank { "বাজার ও দৈনন্দিন খরচ" }

                parsedItems.add(
                    ParsedItem(
                        itemName = itemName,
                        quantity = 1.0,
                        unit = "pcs",
                        unitPrice = primaryAmount,
                        totalPrice = primaryAmount,
                        category = detectCategory(itemName)
                    )
                )
            }
        }

        if (parsedItems.isEmpty()) {
            return AiExtractionResult(
                intent = "unknown",
                replyMessage = "খরচের পরিমাণ বা আইটেম সনাক্ত করা যায়নি। অনুগ্রহ করে টাকার অঙ্ক উল্লেখ করে লিখুন (যেমন: \"৫০০ টাকার মাছ কিনেছি\")।"
            )
        }

        // Calculate deterministic total
        val calculatedTotal = parsedItems.sumOf { it.totalPrice }
        val primaryCategory = parsedItems.firstOrNull()?.category ?: "Groceries"

        val tx = ParsedTransaction(
            id = UUID.randomUUID().toString(),
            type = "EXPENSE",
            date = txDate,
            shopName = extractedShop,
            paymentMethod = paymentMethod,
            currency = "BDT",
            items = parsedItems,
            totalAmount = calculatedTotal,
            notes = trimmed,
            categoryName = primaryCategory
        )

        val itemsSummary = parsedItems.joinToString(", ") { "${it.itemName} (৳${formatNumber(it.totalPrice)})" }
        val shopDetail = if (!extractedShop.isNullOrBlank()) " দোকান: $extractedShop" else ""

        return AiExtractionResult(
            intent = "create_expense",
            language = "bn",
            requiresConfirmation = true,
            transaction = tx,
            replyMessage = "খরচ তৈরি হয়েছে: মোট ৳ ${formatNumber(calculatedTotal)} [$itemsSummary$shopDetail]। সংরক্ষণ করতে \"নিশ্চিত করুন\" বাটনে চাপুন।"
        )
    }

    private fun cleanItemName(raw: String): String {
        return raw.replace(Regex("""(আজকে|গতকাল|আজ|থেকে|কিনেছি|কিনলাম|খরচ|দিলাম|নগদে|বিকাশে|today|yesterday|spent|bought|paid)""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { "আইটেম" }
    }

    private fun detectCategory(item: String): String {
        val lower = item.lowercase()
        return when {
            lower.contains("চাল") || lower.contains("তেল") || lower.contains("মাছ") || lower.contains("মাংস") ||
                    lower.contains("সবজি") || lower.contains("ডিম") || lower.contains("দুধ") || lower.contains("পেঁয়াজ") ||
                    lower.contains("আলু") || lower.contains("grocer") || lower.contains("rice") || lower.contains("oil") ||
                    lower.contains("fish") || lower.contains("meat") || lower.contains("bazaar") || lower.contains("বাজার") -> "Groceries"

            lower.contains("বাস") || lower.contains("রিকশা") || lower.contains("ভাড়া") || lower.contains("উবার") ||
                    lower.contains("সিএনজি") || lower.contains("মেট্রো") || lower.contains("transport") || lower.contains("fare") ||
                    lower.contains("cng") || lower.contains("rickshaw") || lower.contains("uber") -> "Transport"

            lower.contains("খাবার") || lower.contains("চা") || lower.contains("নাস্তা") || lower.contains("বিরিয়ানি") ||
                    lower.contains("বার্গার") || lower.contains("রেস্টুরেন্ট") || lower.contains("food") || lower.contains("dinner") ||
                    lower.contains("lunch") || lower.contains("restaurant") || lower.contains("coffee") -> "Food & Dining"

            lower.contains("ওষুধ") || lower.contains("ডাক্তার") || lower.contains("মেডিসিন") || lower.contains("medicine") ||
                    lower.contains("doctor") || lower.contains("hospital") || lower.contains("medical") -> "Medical & Health"

            lower.contains("বিল") || lower.contains("বিদ্যুৎ") || lower.contains("গ্যাস") || lower.contains("ইন্টারনেট") ||
                    lower.contains("পানি") || lower.contains("wifi") || lower.contains("bill") || lower.contains("electricity") -> "Utilities & Bills"

            lower.contains("শার্ট") || lower.contains("প্যান্ট") || lower.contains("জুতা") || lower.contains("জামা") ||
                    lower.contains("dress") || lower.contains("shirt") || lower.contains("shoes") || lower.contains("shopping") -> "Shopping"

            lower.contains("বাড়ি ভাড়া") || lower.contains("ভাড়া") || lower.contains("rent") -> "House Rent"

            lower.contains("বই") || lower.contains("খাতা") || lower.contains("টিউশন") || lower.contains("স্কুল") ||
                    lower.contains("কলেজ") || lower.contains("education") || lower.contains("school") || lower.contains("book") -> "Education"

            else -> "Other Expense"
        }
    }

    private fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) {
            String.format(Locale.US, "%,.0f", num)
        } else {
            String.format(Locale.US, "%,.2f", num)
        }
    }
}
