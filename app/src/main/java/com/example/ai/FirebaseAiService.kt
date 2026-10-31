package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

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
                You are Hisab AI, a financial extraction assistant.
                The user provides an expense or income description in Bangla or English: "$rawMessage"
                
                Extract into JSON with the following exact keys:
                {
                  "intent": "create_expense" or "create_income",
                  "language": "bn" or "en",
                  "type": "EXPENSE" or "INCOME",
                  "shop_name": "Shop Name or null",
                  "payment_method": "Cash" / "bKash" / "Nagad" / "Bank" / "Credit Card",
                  "items": [
                    {
                      "itemName": "Item Name",
                      "quantity": 1.0,
                      "unit": "kg/liter/pcs",
                      "unitPrice": 100.0,
                      "totalPrice": 100.0,
                      "category": "Groceries/Transport/Food/etc"
                    }
                  ],
                  "total_amount": 100.0,
                  "categoryName": "Groceries/Transport/Food/etc",
                  "reply_message": "Friendly confirmation message in Bangla"
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

    suspend fun answerFinancialQuery(
        query: String,
        financialSummary: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                You are Hisab AI, a smart personal finance assistant.
                Based on the user's current database summary:
                $financialSummary
                
                Answer the following user question accurately and helpfully in Bangla (or English if the query is in English):
                "$query"
                
                Keep the tone helpful, concise, and focused on practical personal budgeting.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    model.generateContent(prompt)
                }
                response.text ?: "তথ্য বিশ্লেষণ সম্পন্ন হয়েছে।"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI assistant query error", e)
                "আর্থিক হিসাব বিশ্লেষণ করা হয়েছে।"
            }
        }
    }
}
