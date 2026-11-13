package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

suspend fun getGeminiResponse(history: List<MessageData>): String {
    return withContext(kotlinx.coroutines.Dispatchers.IO) {
        val systemPrompt = """
            You are Gemini, an intelligent, helpful AI assistant developed by Google.
            Provide clear, accurate, complete, detailed, and well-structured answers to the user's questions.
            Write smoothly with clear formatting and helpful explanations.
        """.trimIndent()

        // Smart history size management to prevent HTTP 413
        val trimmedHistory = if (history.size > 6) history.takeLast(6) else history
        val lastUserIndex = trimmedHistory.indexOfLast { !it.isFromAI }

        val apiMessages = mutableListOf<JsonObject>()
        trimmedHistory.forEachIndexed { index, message ->
            val role = if (message.isFromAI) "model" else "user"
            var content = message.content.trim()

            // Safely limit message length to avoid payload overflow
            if (content.length > 2500 && index != lastUserIndex) {
                content = content.take(2500) + "...\n[Context truncated for size]"
            }

            if (index == lastUserIndex) {
                content += "\n\nPlease provide a comprehensive, detailed, well-formatted, and smooth response."
            }

            apiMessages.add(
                JsonObject(
                    mapOf(
                        "role" to JsonPrimitive(role),
                        "parts" to JsonArray(
                            listOf(JsonObject(mapOf("text" to JsonPrimitive(content))))
                        )
                    )
                )
            )
        }

        val geminiApiKey = "AQ.Ab8RN6LBo4isIYAY4Pg8hzR2ZZ1KRr8rCssYLxR1tzMX3JqyqA"

        val payload = JsonObject(
            mapOf(
                "system_instruction" to JsonObject(
                    mapOf(
                        "parts" to JsonArray(
                            listOf(JsonObject(mapOf("text" to JsonPrimitive(systemPrompt))))
                        )
                    )
                ),
                "contents" to JsonArray(apiMessages),
                "generationConfig" to JsonObject(
                    mapOf(
                        "maxOutputTokens" to JsonPrimitive(4096),
                        "temperature" to JsonPrimitive(0.7)
                    )
                )
            )
        )

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = payload.toString().toRequestBody(mediaType)
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                val request = okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$geminiApiKey")
                    .post(requestBody)
                    .build()

                sharedOkHttpClient.newCall(request).execute().use { response ->
                    val responseText = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val jsonElement = Json.parseToJsonElement(responseText)
                        val rawAnswer = jsonElement.jsonObject["candidates"]
                            ?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("content")
                            ?.jsonObject?.get("parts")
                            ?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("text")
                            ?.jsonPrimitive?.content ?: ""

                        return@withContext if (rawAnswer.isNotBlank()) {
                            cleanResponseText(rawAnswer)
                        } else {
                            "Gemini AI returned an empty response."
                        }
                    } else if (response.code == 429 || response.code == 503) {
                        Log.w("GeminiRateLimit", "Gemini Overloaded (${response.code}), attempt $attempt/$maxRetries")
                        if (attempt < maxRetries) {
                            delay(1500L * attempt)
                        } else {
                            return@withContext "Gemini AI is currently experiencing high demand (Rate Limited). Please try again in a moment."
                        }
                    } else {
                        Log.e("GeminiError", "Gemini API Error ${response.code}: $responseText")
                        return@withContext "Gemini API Error ${response.code}: Unable to process request."
                    }
                }
            } catch (e: Exception) {
                if (attempt == maxRetries) {
                    Log.e("GeminiConnectionError", "Connection failed", e)
                    return@withContext "Gemini Connection Error: ${e.localizedMessage ?: "Unable to contact Gemini API."}"
                }
                delay(1500L * attempt)
            }
        }

        return@withContext "Gemini AI is temporarily unavailable. Please try again."
    }
}

@Composable
fun GeminiNav(
    navController: NavController,
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    checker2: String = "",
    isGlobalSearch: Boolean = false,
    onGlobalSubmit: (String) -> Unit = {},
    externalMessages: SnapshotStateList<MessageData>? = null,
    externalIsLoading: Boolean = false
) {
    val context = LocalContext.current
    var showChat by remember { 
        mutableStateOf(getActiveChat(context, "Gemini").isNotEmpty()) 
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.LightGray
    ) {
        if (showChat) {
            ChatStructure(
                nav = Color(0xFF4285F4),
                platformName = "Gemini",
                searchWord = searchWord,
                onSearchWordChange = onSearchWordChange,
                isGlobalSearch = isGlobalSearch,
                onGlobalSubmit = onGlobalSubmit,
                externalMessages = externalMessages,
                externalIsLoading = externalIsLoading
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GEMINI AI SEARCH",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showChat = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                ) {
                    Text("Search with Gemini")
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Back to Dashboard", color = Color.Gray)
                }
            }
        }
    }
}
