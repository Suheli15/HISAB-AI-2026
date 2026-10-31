package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAiService {
    private const val TAG = "GeminiAiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseWithGeminiOrFallback(
        rawMessage: String,
        apiKey: String?,
        model: String = "gemini-2.5-flash",
        pendingTransaction: ParsedTransaction? = null
    ): Pair<AiExtractionResult, Long> {
        val effectiveKey = apiKey?.takeIf { it.isNotBlank() }
            ?: (try { BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String } catch (e: Exception) { null })?.takeIf { it.isNotBlank() && !it.contains("MY_GEMINI_API_KEY") }

        // If no active Gemini API key is configured, instantly use high-accuracy local parser
        if (effectiveKey.isNullOrBlank()) {
            val localResult = BanglaNlpParser.parseMessage(rawMessage, pendingTransaction)
            return Pair(localResult, 0L)
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$effectiveKey"

                val systemPrompt = """
                You are Hisab AI, a financial extraction assistant. The user provides an expense or income description in Bangla or English.
                Extract:
                - intent: "create_expense" or "create_income"
                - language: "bn" or "en"
                - type: "EXPENSE" or "INCOME"
                - shop_name: string or null
                - payment_method: "Cash", "bKash", "Nagad", "Bank", "Credit Card"
                - items: array of { "itemName": string, "quantity": number, "unit": string, "unitPrice": number, "totalPrice": number, "category": string }
                - total_amount: number (sum of items)
                - notes: original user text
                - categoryName: primary category
                - reply_message: friendly confirmation in Bangla
                
                Respond ONLY with raw JSON matching this format without markdown code blocks.
                """.trimIndent()

                val prompt = "User message: \"$rawMessage\"\nContext: Extract financial transaction in JSON."

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "$systemPrompt\n\n$prompt"))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.1)
                        put("responseMimeType", "application/json")
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val contentObj = candidates?.optJSONObject(0)?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val rawText = parts?.optJSONObject(0)?.optString("text")

                    val usageMetadata = root.optJSONObject("usageMetadata")
                    val tokenCount = usageMetadata?.optLong("totalTokenCount", 120L) ?: 120L

                    if (!rawText.isNullOrBlank()) {
                        val parsedJson = JSONObject(rawText.trim().removePrefix("```json").removeSuffix("```").trim())
                        val itemsArray = parsedJson.optJSONArray("items")
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

                        val totalAmount = if (items.isNotEmpty()) items.sumOf { it.totalPrice } else parsedJson.optDouble("total_amount", 0.0)

                        val tx = ParsedTransaction(
                            type = parsedJson.optString("type", "EXPENSE").uppercase(),
                            date = parsedJson.optString("date", BanglaNlpParser.convertBanglaDigits(rawMessage)),
                            shopName = parsedJson.optString("shop_name", null)?.takeIf { it != "null" && it.isNotBlank() },
                            paymentMethod = parsedJson.optString("payment_method", "Cash"),
                            currency = "BDT",
                            items = items,
                            totalAmount = totalAmount,
                            notes = rawMessage,
                            categoryName = parsedJson.optString("categoryName", "Groceries")
                        )

                        val result = AiExtractionResult(
                            intent = parsedJson.optString("intent", "create_expense"),
                            language = parsedJson.optString("language", "bn"),
                            requiresConfirmation = true,
                            transaction = tx,
                            replyMessage = parsedJson.optString("reply_message", "হিসাবটি পাওয়া গেছে। অনুগ্রহ করে তথ্য যাচাই করে সংরক্ষণ করুন।")
                        )
                        return@withContext Pair(result, tokenCount)
                    }
                }

                // If error or empty response, gracefully fallback
                Log.w(TAG, "Gemini API non-200 or empty, falling back to local NLP. Status: ${response.code}")
                Pair(BanglaNlpParser.parseMessage(rawMessage, pendingTransaction), 0L)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API request failed, falling back to local NLP: ${e.message}")
                Pair(BanglaNlpParser.parseMessage(rawMessage, pendingTransaction), 0L)
            }
        }
    }

    suspend fun testConnection(apiKey: String, model: String = "gemini-2.5-flash"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "ping"))
                            })
                        })
                    })
                }
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }
}
