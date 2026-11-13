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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

suspend fun getGrokResponse(history: List<MessageData>): String {
    val apiMessages = mutableListOf<JsonObject>()

    apiMessages.add(
        JsonObject(
            mapOf(
                "role" to JsonPrimitive("system"),
                "content" to JsonPrimitive("You are Grok, an intelligent AI assistant. Provide clear, detailed, comprehensive, and well-explained answers.")
            )
        )
    )

    // Smart context length management to prevent HTTP 413 Payload Too Large
    val trimmedHistory = if (history.size > 5) history.takeLast(5) else history
    val lastUserIndex = trimmedHistory.indexOfLast { !it.isFromAI }

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

    val payload = JsonObject(
        mapOf(
            "model" to JsonPrimitive("qwen/qwen3.8-27b"),
            "messages" to JsonArray(apiMessages),
            "max_completion_tokens" to JsonPrimitive(2048),
            "temperature" to JsonPrimitive(0.7)
        )
    )

    val secureApiKey = "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"
    val maxRetries = 6

    for (attempt in 1..maxRetries) {
        try {
            val response: HttpResponse = chatClient.post("https://api.groq.com/openai/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $secureApiKey")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status == HttpStatusCode.OK) {
                val jsonElement = Json.parseToJsonElement(responseBody)
                val rawAnswer = jsonElement
                    .jsonObject["choices"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonObject
                    ?.get("content")
                    ?.jsonPrimitive
                    ?.content
                    ?: "No response received."

                return cleanResponseText(rawAnswer)
            } else if (response.status.value == 429 || response.status.value == 503) {
                Log.w("GrokRateLimit", "Grok Rate Limited (429/503), attempt $attempt/$maxRetries. Waiting to retry...")

                if (attempt < maxRetries) {
                    val retryAfterHeader = response.headers["Retry-After"]
                    var delayMs = 3000L * attempt

                    if (!retryAfterHeader.isNullOrBlank()) {
                        val headerSec = retryAfterHeader.toDoubleOrNull()
                        if (headerSec != null) {
                            delayMs = (headerSec * 1000).toLong() + 500L
                        }
                    } else if (responseBody.contains("try again in")) {
                        val match = Regex("try again in (\\d+(\\.\\d+)?)s").find(responseBody)
                        if (match != null) {
                            val sec = match.groupValues[1].toDoubleOrNull()
                            if (sec != null) {
                                delayMs = (sec * 1000).toLong() + 500L
                            }
                        }
                    }

                    delay(delayMs)
                }
            } else {
                Log.e("GrokError", "Grok Error ${response.status.value}: $responseBody")
            }
        } catch (e: Exception) {
            Log.e("GrokConnectionError", "Grok attempt $attempt failed", e)
            if (attempt < maxRetries) {
                delay(2000L * attempt)
            }
        }
    }

    // Seamless Fallback to Gemini when rate-limited so user never sees a rate-limit error!
    return try {
        getGeminiResponse(history)
    } catch (e: Exception) {
        "Grok is temporarily busy. Please ask your question again in a moment."
    }
}

@Composable
fun GrokNav(
    navController: NavController,
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    isGlobalSearch: Boolean = false,
    onGlobalSubmit: (String) -> Unit = {},
    externalMessages: androidx.compose.runtime.snapshots.SnapshotStateList<MessageData>? = null,
    externalIsLoading: Boolean = false
) {
    val context = LocalContext.current
    var isPressed by remember { 
        mutableStateOf(getActiveChat(context, "Grok").isNotEmpty()) 
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.LightGray
    ) {
        if (isPressed) {
            ChatStructure(
                nav = Color(0xFF6366F1),
                platformName = "Grok",
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
                    text = "GROK AI SEARCH",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { isPressed = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Search with Grok")
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Back to Dashboard", color = Color.Gray)
                }
            }
        }
    }
}
