package com.example.myapplication

import android.util.Log
import kotlinx.serialization.json.*
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import kotlinx.coroutines.delay
import okhttp3.RequestBody.Companion.toRequestBody



suspend fun getGeminiResponse(history: List<MessageData>): String {
    return withContext(Dispatchers.IO) {

        val systemPrompt = """
            You are Gemini, an intelligent, helpful AI assistant developed by Google.
            Provide clear, accurate, complete, detailed, and well-structured answers.
            Use smooth language, helpful explanations, and appropriate formatting.
            Maintain context from previous messages in the conversation.
        """.trimIndent()

        // Keep the most recent six messages.
        val trimmedHistory = history
            .filter { it.content.isNotBlank() }
            .takeLast(6)

        // Gemini conversations should begin with a user message.
        val firstUserIndex = trimmedHistory.indexOfFirst {
            !it.isFromAI
        }

        if (firstUserIndex == -1) {
            return@withContext "Please enter a message before contacting Gemini."
        }

        val lastUserIndex = trimmedHistory.indexOfLast {
            !it.isFromAI
        }

        val apiMessages = buildJsonArray {

            trimmedHistory
                .drop(firstUserIndex)
                .forEachIndexed { index, message ->

                    var content = message.content.trim()

                    // Limit older messages to reduce request size.
                    val originalIndex = index + firstUserIndex

                    if (content.length > 2500 &&
                        originalIndex != lastUserIndex
                    ) {
                        content = content.take(2500) +
                                "\n[Context shortened]"
                    }

                    // Improve the latest user prompt.
                    if (originalIndex == lastUserIndex) {
                        content += "\n\nProvide a comprehensive, clear, and well-formatted answer."
                    }

                    add(
                        buildJsonObject {
                            put(
                                "role",
                                if (message.isFromAI) "model" else "user"
                            )

                            putJsonArray("parts") {
                                add(
                                    buildJsonObject {
                                        put("text", content)
                                    }
                                )
                            }
                        }
                    )
                }
        }

        // IMPORTANT:
        // Replace this placeholder with a NEW key only if testing
        // direct API access. Never reuse the key you exposed.
        // For production, keep the key in your backend instead.
        val geminiApiKey = AppKeyStore.get("gemini")

        if (geminiApiKey == "PASTE_YOUR_NEW_API_KEY_HERE" ||
            geminiApiKey.isBlank()

        ) {
            return@withContext "Please configure your new Gemini API key."
        }

        val payload = buildJsonObject {

            put(
                "systemInstruction",
                buildJsonObject {
                    putJsonArray("parts") {
                        add(
                            buildJsonObject {
                                put("text", systemPrompt)
                            }
                        )
                    }
                }
            )

            put("contents", apiMessages)

            put(
                "generationConfig",
                buildJsonObject {
                    put("maxOutputTokens", 4096)
                    put("temperature", 0.7)
                }
            )
        }

        val requestBody = payload.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaTypeOrNull()
        )

        // Current Gemini model IDs.
        val geminiModels = listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite"
        )

        var lastErrorMsg = "Gemini AI is temporarily unavailable."

        for (modelName in geminiModels) {

            try {

                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", geminiApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                sharedOkHttpClient.newCall(request).execute().use { response ->

                    val responseText = response.body?.string() ?: ""

                    if (response.isSuccessful) {

                        val jsonElement =
                            Json.parseToJsonElement(responseText)

                        val candidates = jsonElement
                            .jsonObject["candidates"]
                            ?.jsonArray

                        val rawAnswer = candidates
                            ?.firstOrNull()
                            ?.jsonObject
                            ?.get("content")
                            ?.jsonObject
                            ?.get("parts")
                            ?.jsonArray
                            ?.mapNotNull { part ->
                                part.jsonObject["text"]
                                    ?.jsonPrimitive
                                    ?.content
                            }
                            ?.joinToString("\n")
                            ?.trim()
                            ?: ""

                        if (rawAnswer.isNotBlank()) {
                            return@withContext cleanResponseText(rawAnswer)
                        }

                        lastErrorMsg =
                            "Gemini returned an empty response. Please try again."

                    } else {

                        Log.e(
                            "GeminiAPI",
                            "Model: $modelName | HTTP ${response.code} | $responseText"
                        )

                        when (response.code) {

                            400 -> {
                                lastErrorMsg =
                                    "Gemini request error (400): $responseText"
                                return@withContext lastErrorMsg
                            }

                            401, 403 -> {
                                lastErrorMsg =
                                    "Gemini authentication error (${response.code}). Check your API key and Google AI Studio access."
                                return@withContext lastErrorMsg
                            }

                            404 -> {
                                lastErrorMsg =
                                    "Gemini model $modelName is unavailable. Trying the next model."
                            }

                            429, 503 -> {
                                lastErrorMsg =
                                    "Gemini is busy or rate limited (${response.code}). Trying the next model."
                            }

                            else -> {
                                lastErrorMsg =
                                    "Gemini API error (${response.code}): $responseText"
                                return@withContext lastErrorMsg
                            }
                        }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "GeminiError",
                    "Connection error on $modelName",
                    e
                )

                lastErrorMsg =
                    "Gemini connection error: ${e.localizedMessage ?: "Unable to connect"}"
            }
        }

        lastErrorMsg
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
        color = Color(0xFFF5F7F9)
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
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFE8F0FE)
                        ) {
                            Text(
                                text = "GEMINI 1.5 FLASH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4285F4),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Gemini AI",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Detailed, smooth answers for brainstorming, coding & everyday tasks.",
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Text("Start Gemini Chat 🚀", fontWeight = FontWeight.Bold, color = Color.White)
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
