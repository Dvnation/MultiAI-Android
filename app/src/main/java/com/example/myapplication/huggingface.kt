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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
    externalMessages: SnapshotStateList<MessageData>? = null,
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
        color = Color(0xFFF5F7F9)
    ) {
        if (showChat) {
            ChatStructure(
                nav = Color(0xFFFF6D00),
                platformName = "Hugging Face",
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤗", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = "OPEN AI ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF6D00),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Hugging Face AI",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Clear, structured answers with smart context handling.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showChat = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6D00)
                            )
                        ) {
                            Text("Start Hugging Face Chat ⚡", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text("Back to Dashboard", color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}