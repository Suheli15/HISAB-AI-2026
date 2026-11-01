package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FirebaseAiService {
    private const val TAG = "FirebaseAiService"

    // Mandated: Lazy evaluation prevents startup crashes before App Check completes
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun parseWithGemini(
        rawMessage: String,
        pendingTransaction: ParsedTransaction? = null
    ): Pair<AiExtractionResult, Long> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                You are Hisab AI, a specialized personal finance and expense extraction assistant.
                The user provides an expense or income description in Bangla, Banglish, or English: "$rawMessage"
                
                Carefully understand Bengali numerals (০-৯) or English numerals, local shopping phrases (e.g. চাল, ডাল, তেল, ডিম, মাছ, মাংস, কাঁচাবাজার, রিকশা ভাড়া, উবার, সিএনজি, চা-নাস্তা, ওষুধ, বাড়ি ভাড়া, বেতন, টিউশন ফি).
                
                Extract into JSON with the following exact keys:
                {
                  "intent": "create_expense" or "create_income",
                  "language": "bn" or "en",
                  "type": "EXPENSE" or "INCOME",
                  "shop_name": "Shop/Store Name or null",
                  "payment_method": "Cash" / "bKash" / "Nagad" / "Bank" / "Credit Card",
                  "items": [
                    {
                      "itemName": "Item Name in original language",
                      "quantity": 1.0,
                      "unit": "kg/liter/pcs/packet/dozen",
                      "unitPrice": 100.0,
                      "totalPrice": 100.0,
                      "category": "Groceries/Transport/Food/Shopping/Medical/Utilities/Salary/etc"
                    }
                  ],
                  "total_amount": 100.0,
                  "categoryName": "Primary category",
                  "reply_message": "Warm, encouraging confirmation message in Bengali summarizing the items and total"
                }
                
                Return ONLY valid raw JSON without markdown code fences or backticks.
                """.trimIndent()

                // Mandated: realistic 30-60s timeout for model inference
                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }

                val responseText = response.text
                if (!responseText.isNullOrBlank()) {
                    val cleanJson = responseText.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val root = JSONObject(cleanJson)
                    val itemsArray = root.optJSONArray("items")
                    val items = mutableListOf<ParsedItem>()
                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(i)
                            items.add(
                                ParsedItem(
                                    itemName = itemObj.optString("itemName", "Item"),
                                    quantity = itemObj.optDouble("quantity", 1.0),
                                    unit = itemObj.optString("unit", "pcs"),
                                    unitPrice = itemObj.optDouble("unitPrice", 0.0),
                                    totalPrice = itemObj.optDouble("totalPrice", 0.0),
                                    category = itemObj.optString("category", "Groceries")
                                )
                            )
                        }
                    }

                    val total = if (items.isNotEmpty()) items.sumOf { it.totalPrice } else root.optDouble("total_amount", 0.0)

                    val tx = ParsedTransaction(
                        type = root.optString("type", "EXPENSE").uppercase(),
                        date = root.optString("date", BanglaNlpParser.convertBanglaDigits(rawMessage)),
                        shopName = root.optString("shop_name", null)?.takeIf { it != "null" && it.isNotBlank() },
                        paymentMethod = root.optString("payment_method", "Cash"),
                        currency = "BDT",
                        items = items,
                        totalAmount = total,
                        notes = rawMessage,
                        categoryName = root.optString("categoryName", "Groceries")
                    )

                    val result = AiExtractionResult(
                        intent = root.optString("intent", "create_expense"),
                        language = root.optString("language", "bn"),
                        requiresConfirmation = true,
                        transaction = tx,
                        replyMessage = root.optString("reply_message", "হিসাবটি পাওয়া গেছে। তথ্য যাচাই করে সংরক্ষণ করুন।")
                    )
                    return@withContext Pair(result, 150L)
                }

                Pair(BanglaNlpParser.parseMessage(rawMessage, pendingTransaction), 0L)
            } catch (e: CancellationException) {
                // ALWAYS re-throw CancellationException to preserve cooperative cancellation
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI Logic inference error, falling back to local NLP", e)
                Pair(BanglaNlpParser.parseMessage(rawMessage, pendingTransaction), 0L)
            }
        }
    }

    suspend fun analyzeReceiptImage(bitmap: Bitmap): ParsedTransaction? {
        return withContext(Dispatchers.IO) {
            try {
                val response = withTimeout(60_000L) {
                    model.generateContent(
                        content {
                            image(bitmap)
                            text("""
                            Analyze this receipt/cash memo image carefully.
                            Extract:
                            1. Store or merchant name (if visible).
                            2. Date of receipt in YYYY-MM-DD (or current date if not visible).
                            3. Individual purchased items with quantity, unit price, total price, and appropriate category.
                            4. Total invoice/memo amount.
                            
                            Return strictly raw JSON format without markdown fences:
                            {
                              "shop_name": "Merchant Name",
                              "date": "YYYY-MM-DD",
                              "items": [
                                {
                                  "itemName": "Item name",
                                  "quantity": 1.0,
                                  "unit": "pcs",
                                  "unitPrice": 100.0,
                                  "totalPrice": 100.0,
                                  "category": "Groceries"
                                }
                              ],
                              "total_amount": 100.0
                            }
                            """.trimIndent())
                        }
                    )
                }

                val text = response.text
                if (!text.isNullOrBlank()) {
                    val cleanJson = text.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val root = JSONObject(cleanJson)
                    val itemsArray = root.optJSONArray("items")
                    val items = mutableListOf<ParsedItem>()
                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val obj = itemsArray.getJSONObject(i)
                            items.add(
                                ParsedItem(
                                    itemName = obj.optString("itemName", "Item"),
                                    quantity = obj.optDouble("quantity", 1.0),
                                    unit = obj.optString("unit", "pcs"),
                                    unitPrice = obj.optDouble("unitPrice", 0.0),
                                    totalPrice = obj.optDouble("totalPrice", 0.0),
                                    category = obj.optString("category", "Groceries")
                                )
                            )
                        }
                    }
                    val total = if (items.isNotEmpty()) items.sumOf { it.totalPrice } else root.optDouble("total_amount", 0.0)

                    return@withContext ParsedTransaction(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        date = root.optString("date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())),
                        shopName = root.optString("shop_name", "রসিদ / মেমো"),
                        paymentMethod = "Cash",
                        currency = "BDT",
                        items = items,
                        totalAmount = total,
                        notes = "রসিদের ছবি থেকে এআই স্ক্যানকৃত",
                        categoryName = items.firstOrNull()?.category ?: "Groceries"
                    )
                }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Receipt multimodal image analysis failed", e)
                null
            }
        }
    }

    suspend fun generateBudgetAdvice(
        monthlyBudget: Double,
        spentThisMonth: Double,
        remainingDays: Int,
        dailySafeBudget: Double,
        todaySpent: Double,
        topCategory: String?
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                You are a senior personal finance advisor in Bangladesh.
                The user has the following real financial stats for the current month:
                - Monthly Total Budget: ৳ $monthlyBudget
                - Total Spent so far: ৳ $spentThisMonth
                - Remaining Monthly Budget: ৳ ${monthlyBudget - spentThisMonth}
                - Remaining Days in this month: $remainingDays days
                - Recommended Daily Safe Spending Limit: ৳ $dailySafeBudget / day
                - Spent Today so far: ৳ $todaySpent
                - Top Expense Category: ${topCategory ?: "None"}
                
                Provide a short, direct, actionable 3-4 sentence budget advice in friendly Bengali:
                1. How their current pace compares to their monthly target.
                2. Explicit daily spending guidance for the remaining $remainingDays days.
                3. One actionable tip to stay within limits or save.
                """.trimIndent()

                val response = withTimeout(40_000L) {
                    model.generateContent(prompt)
                }
                response.text ?: "বাজেট অনুযায়ী আপনার দৈনিক খরচ নিরাপদ সীমার মধ্যে রাখার চেষ্টা করুন।"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Budget advice generation error", e)
                "আপনার অবশিষ্ট বাজেটের ভিত্তিতে প্রতিদিন প্রায় ৳ ${String.format(Locale.US, "%,.0f", dailySafeBudget)} খরচ করলে মাস শেষে বাজেট অতিরিক্ত হবে না।"
            }
        }
    }
}
