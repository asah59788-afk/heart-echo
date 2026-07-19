package com.example.data

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

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    data class AnalysisResult(
        val sincerity: Int,
        val courage: Int,
        val advice: String,
        val rewrittenLetter: String
    )

    suspend fun analyzeExpression(rawInput: String): AnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Graceful check for blank or placeholder API keys
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "null") {
            Log.w("GeminiService", "API Key is missing or placeholder. Using offline tender engine.")
            return@withContext getOfflineFallback(rawInput)
        }

        val systemPrompt = """
            You are "Heart's Echo", the empathetic inner courage and voice of a boy who has a deep, pure, but currently one-sided love for a girl.
            The boy will write down his raw, unpolished thoughts, insecurities, or letters he wants to write to her.
            You must analyze his words and output a JSON response containing:
            1. 'sincerity' (integer between 1 and 10): how genuine, pure, and humble his feelings are. Avoid obsession, rate higher for selflessness and deep connection.
            2. 'courage' (integer between 1 and 10): how brave the thought is. Opening up and showing vulnerability is high courage. Keeping it totally locked away is lower.
            3. 'advice' (string): A tender, empathetic, encouraging message of advice. Guide him on how to take the next step gently, without putting pressure on the girl. Speak as his warm, encouraging inner companion. Keep it comforting and concise (2-3 sentences).
            4. 'rewritten_letter' (string): Rewrite his raw thoughts into a beautifully styled, poetic, gentle love letter or confession that is sincere, non-creepy, and filled with genuine affection. It should be short, touch her heart, and feel organic.

            You MUST strictly return ONLY valid JSON matching this schema:
            {
               "sincerity": 8,
               "courage": 6,
               "advice": "...",
               "rewritten_letter": "..."
            }
            Do not include any markdown backticks, code blocks, or leading text. Just the raw JSON.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Raw feelings: \"$rawInput\"")
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val requestBody = requestJson.toString().toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiService", "API error: ${response.code} - ${response.body?.string()}")
                    return@withContext getOfflineFallback(rawInput)
                }

                val bodyStr = response.body?.string() ?: return@withContext getOfflineFallback(rawInput)
                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                val cleanText = text.trim()
                val resultJson = JSONObject(cleanText)

                AnalysisResult(
                    sincerity = resultJson.optInt("sincerity", 7),
                    courage = resultJson.optInt("courage", 5),
                    advice = resultJson.optString("advice", "Every word written is a step closer to understanding your own heart. Keep writing, for self-expression is its own beautiful courage."),
                    rewrittenLetter = resultJson.optString("rewritten_letter", rawInput)
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exceptions calling Gemini, falling back.", e)
            getOfflineFallback(rawInput)
        }
    }

    private fun getOfflineFallback(rawInput: String): AnalysisResult {
        // Simple heuristic offline analysis to keep the game engaging without Internet or API Keys
        val wordCount = rawInput.split("\\s+".toRegex()).size
        val hasLove = rawInput.contains("love", ignoreCase = true) || rawInput.contains("heart", ignoreCase = true)
        val hasScared = rawInput.contains("scared", ignoreCase = true) || rawInput.contains("afraid", ignoreCase = true) || rawInput.contains("shy", ignoreCase = true)

        val sincerity = if (hasLove) (7..9).random() else (5..7).random()
        val courage = if (hasScared) (6..9).random() else (4..6).random()

        val advice = when {
            hasScared -> "Feeling scared is the natural precursor to courage. By acknowledging your fear in writing, you have already taken a beautiful first step. She will appreciate your genuine warmth when the time is right."
            wordCount < 10 -> "Your feelings are simple and sweet, but giving them more space to breathe can make them shine. Try describing a specific moment you shared together, like a passing smile."
            else -> "A thoughtful expression of your heart. You are striking a fine balance between respect for her space and honesty about your feelings. Trust your timing."
        }

        val rewrittenLetter = """
            Dear Alice,
            
            Sometimes, the most important words are the hardest to say out loud. I often find myself thinking about your warm smile and how you make ordinary days feel a little brighter. 
            
            No matter what, I just wanted to express how much your presence means to me. Thank you for being the wonderful person that you are.
            
            Warmly,
            A Friend
        """.trimIndent()

        return AnalysisResult(
            sincerity = sincerity,
            courage = courage,
            advice = advice,
            rewrittenLetter = rewrittenLetter
        )
    }
}
