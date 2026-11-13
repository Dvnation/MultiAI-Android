package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private val hfOkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .callTimeout(50, TimeUnit.SECONDS)
    .build()

suspend fun getHuggingFaceResponse(history: List<MessageData>): String {
    return withContext(Dispatchers.IO) {
        val trimmedHistory = if (history.size > 5) history.takeLast(5) else history
        val lastUserIndex = trimmedHistory.indexOfLast { !it.isFromAI }
        val apiMessages = mutableListOf<JsonObject>()

        apiMessages.add(
            JsonObject(
                mapOf(
                    "role" to JsonPrimitive("system"),
                    "content" to JsonPrimitive("You are a helpful, intelligent AI assistant. Provide clear, detailed, comprehensive, well-structured answers.")
                )
            )
        )

        trimmedHistory.forEachIndexed { index, message ->
            if (message.content.isNotBlank()) {
                val role = if (message.isFromAI) "assistant" else "user"
                var content = message.content.trim()

                if (content.length > 2000 && index != lastUserIndex) {
                    content = content.take(2000) + "...\n[Context truncated to prevent 413 error]"
                }

                apiMessages.add(
                    JsonObject(
                        mapOf(
                            "role" to JsonPrimitive(role),
                            "content" to JsonPrimitive(content)
                        )
                    )
                )
            }
        }

        val groqApiKey = "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"

        val payload = JsonObject(
            mapOf(
                "model" to JsonPrimitive("qwen/qwen3.8-27b"),
                "messages" to JsonArray(apiMessages),
                "max_completion_tokens" to JsonPrimitive(2048),
                "temperature" to JsonPrimitive(0.7)
            )
        )

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val maxRetries = 6

        for (attempt in 1..maxRetries) {
            try {
                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer $groqApiKey")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                hfOkHttpClient.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        val json = Json.parseToJsonElement(responseText)
                        val answer = json.jsonObject["choices"]
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                            ?.get("message")
                            ?.jsonObject
                            ?.get("content")
                            ?.jsonPrimitive
                            ?.content
                            .orEmpty()

                        return@withContext if (answer.isNotBlank()) {
                            cleanResponseText(answer)
                        } else {
                            "Hugging Face returned an empty response."
                        }
                    } else if (response.code == 429 || response.code == 503) {
                        Log.w("HFRateLimit", "Hugging Face Rate Limited (${response.code}), attempt $attempt/$maxRetries. Waiting to retry...")

                        if (attempt < maxRetries) {
                            val retryAfterHeader = response.header("Retry-After")
                            var delayMs = 3000L * attempt

                            if (!retryAfterHeader.isNullOrBlank()) {
                                val headerSec = retryAfterHeader.toDoubleOrNull()
                                if (headerSec != null) {
                                    delayMs = (headerSec * 1000).toLong() + 500L
                                }
                            }

                            delay(delayMs)
                        }
                    } else {
                        Log.e("HFError", "Hugging Face Error ${response.code}: $responseText")
                    }
                }
            } catch (e: Exception) {
                Log.e("HFConnectionError", "Hugging Face attempt $attempt failed", e)
                if (attempt < maxRetries) {
                    delay(2000L * attempt)
                }
            }
        }

        // Seamless Fallback to Gemini when rate-limited so user never sees a rate-limit error!
        return@withContext try {
            getGeminiResponse(history)
        } catch (e: Exception) {
            "Hugging Face is temporarily busy. Please try again in a moment."
        }
    }
}

@Composable
fun HuggingFaceNav(
    navController: NavController,
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    isGlobalSearch: Boolean = false,
    onGlobalSubmit: (String) -> Unit = {},
    externalMessages: androidx.compose.runtime.snapshots.SnapshotStateList<MessageData>? = null,
    externalIsLoading: Boolean = false
) {
    val context = LocalContext.current

    var showChat by remember {
        mutableStateOf(
            getActiveChat(context, "Hugging Face").isNotEmpty()
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.LightGray
    ) {
        if (showChat) {
            ChatStructure(
                nav = Color(0xFF05040E),
                platformName = "Hugging Face",
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
                    text = "HUGGING FACE AI SEARCH",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF05040E)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showChat = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF05040E)
                    )
                ) {
                    Text("Search with Hugging Face")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Back to Dashboard", color = Color.Gray)
                }
            }
        }
    }
}
