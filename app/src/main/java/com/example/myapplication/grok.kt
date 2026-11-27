package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
            "model" to JsonPrimitive("openai/gpt-oss-20b"),
            "messages" to JsonArray(apiMessages),
            "max_completion_tokens" to JsonPrimitive(2048),
            "temperature" to JsonPrimitive(0.7)
        )
    )

    val secureApiKey = AppKeyStore.get("openrouter")
    if (secureApiKey.isBlank()) return "OpenRouter API key is not configured. Add your key in Settings."
    val maxRetries = 3

    for (attempt in 1..maxRetries) {
        try {
            val response: HttpResponse = chatClient.post("https://openrouter.ai/api/v1/chat/completions") {
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

    return "Grok is temporarily unavailable or rate-limited. Please try again shortly."

}

@Composable
fun GrokNav(
    navController: NavController,
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    isGlobalSearch: Boolean = false,
    onGlobalSubmit: (String) -> Unit = {},
    externalMessages: SnapshotStateList<MessageData>? = null,
    externalIsLoading: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var isPressed by remember {
        mutableStateOf(getActiveChat(context, "Grok").isNotEmpty())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7F9)
    ) {
        if (isPressed) {
            ChatStructure(
                nav = Color(0xFF008A50),
                platformName = "Grok",
                searchWord = searchWord,
                onSearchWordChange = onSearchWordChange,
                isGlobalSearch = isGlobalSearch,
                onGlobalSubmit = onGlobalSubmit,
                externalMessages = externalMessages,
                externalIsLoading = externalIsLoading
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE6FBF1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 30.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFE6FBF1)
                        ) {
                            Text(
                                text = "GROK ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF008A50),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "GROK AI SEARCH",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Fast, sharp responses with automatic fallback protection.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isPressed = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B86B))
                        ) {
                            Text(
                                text = "Search with Grok 🚀",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Back to Dashboard", color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CerebrasNav(
    navController: NavController,
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    isGlobalSearch: Boolean = false,
    onGlobalSubmit: (String) -> Unit = {},
    externalMessages: SnapshotStateList<MessageData>? = null,
    externalIsLoading: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var isPressed by remember {
        mutableStateOf(getActiveChat(context, "Cerebras").isNotEmpty())
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7F9)
    ) {
        if (isPressed) {
            ChatStructure(
                nav = Color(0xFF6D5DFB),
                platformName = "Cerebras",
                searchWord = searchWord,
                onSearchWordChange = onSearchWordChange,
                isGlobalSearch = isGlobalSearch,
                onGlobalSubmit = onGlobalSubmit,
                externalMessages = externalMessages,
                externalIsLoading = externalIsLoading
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDEBFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 30.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFEDEBFF)
                        ) {
                            Text(
                                text = "CEREBRAS ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6D5DFB),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "CEREBRAS AI SEARCH",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Fast, sharp responses with automatic fallback protection.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isPressed = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D5DFB))
                        ) {
                            Text(
                                text = "Search with Cerebras 🚀",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Back to Dashboard", color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
