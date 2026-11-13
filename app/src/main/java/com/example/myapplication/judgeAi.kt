package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

fun getLatestAiResponses(context: Context): Map<String, String> {
    val platforms = listOf("Gemini", "Hugging Face", "Grok")
    val map = mutableMapOf<String, String>()
    for (platform in platforms) {
        val messages = getActiveChat(context, platform)
        var lastAiMessage = messages.lastOrNull { it.isFromAI }?.content
            ?: messages.lastOrNull { !it.isFromAI }?.content
            ?: "No recent response recorded for $platform."

        // Smart payload length management to prevent HTTP 413 Payload Too Large for Judge AI
        if (lastAiMessage.length > 1500) {
            lastAiMessage = lastAiMessage.take(1500) + "...\n[Response summary truncated for evaluation]"
        }
        map[platform] = lastAiMessage
    }
    return map
}

suspend fun getCloudflareJudgeResponse(history: List<MessageData>): String {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val apiMessages = mutableListOf<JsonObject>()

            apiMessages.add(
                JsonObject(
                    mapOf(
                        "role" to JsonPrimitive("system"),
                        "content" to JsonPrimitive(
                            """
                            You are Cloudflare Judge AI, an expert analytical AI evaluator powered by Cloudflare Workers AI.
                            Your role is to compare, contrast, or merge responses provided from Gemini, Hugging Face, and Grok.
                            Provide clear, unbiased, well-structured responses with good spacing and formatting.
                            Avoid unnecessary hashtags or markdown noise.
                            """.trimIndent()
                        )
                    )
                )
            )

            // Smart history size management to prevent HTTP 413 Payload Too Large
            val trimmedHistory = if (history.size > 5) history.takeLast(5) else history
            val hiddenPrompt = "\n\n(Note: Please make the response very smooth, detailed, clear, and well-formatted.)"
            val lastUserIndex = trimmedHistory.indexOfLast { !it.isFromAI }

            trimmedHistory.forEachIndexed { index, message ->
                if (message.content.isNotBlank()) {
                    val role = if (message.isFromAI) "assistant" else "user"
                    var content = message.content.trim()

                    if (content.length > 2000 && index != lastUserIndex) {
                        content = content.take(2000) + "...\n[Context truncated to prevent 413 error]"
                    }

                    if (index == lastUserIndex) {
                        content += hiddenPrompt
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

            val accountId = "c5950e189d2d0c1e8d6411f75e110ebf"
            val targetUrl = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/@cf/meta/llama-3.1-8b-instruct"
            val cfToken = "vXf6sZ9k0_mU2qW5eP8jL1nB4vC7xY3aZ0qW8eR2"

            val payload = JsonObject(
                mapOf(
                    "messages" to JsonArray(apiMessages),
                    "max_tokens" to JsonPrimitive(2048)
                )
            )

            val rawOkHttpClient = okhttp3.OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = payload.toString().toRequestBody(mediaType)

            val request = okhttp3.Request.Builder()
                .url(targetUrl)
                .addHeader("Authorization", "Bearer $cfToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            rawOkHttpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    try {
                        val jsonElement = Json.parseToJsonElement(responseText)
                        val resultObj = jsonElement.jsonObject["result"]?.jsonObject
                        val answer = resultObj?.get("response")?.jsonPrimitive?.content
                            ?: jsonElement.jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content

                        if (!answer.isNullOrBlank()) {
                            cleanResponseText(answer)
                        } else {
                            "Cloudflare AI returned an empty response."
                        }
                    } catch (e: Exception) {
                        "Cloudflare AI Parsing Error: ${e.localizedMessage}"
                    }
                } else {
                    getGrokResponse(history)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                getGrokResponse(history)
            } catch (ex: Exception) {
                "Cloudflare AI Error: ${e.localizedMessage}"
            }
        }
    }
}

@Composable
fun JudgeAiNav(
    navController: NavController,
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    isGlobalSearch: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onGlobalSubmit: (String) -> Unit = {},
    externalMessages: androidx.compose.runtime.snapshots.SnapshotStateList<MessageData>? = null
) {
    val judgeColor = Color(0xFF8B5CF6)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.LightGray
    ) {
        if (!isGlobalSearch) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = judgeColor,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "JUDGE AI IS DISABLED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = judgeColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "To compare, contrast, and merge responses across AI models, please enable 'Same Search Across AI Platforms' in Settings.",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = judgeColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Go to Settings", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else {
            ChatStructure(
                nav = judgeColor,
                platformName = "Judge AI",
                searchWord = searchWord,
                onSearchWordChange = onSearchWordChange,
                isGlobalSearch = isGlobalSearch,
                onGlobalSubmit = onGlobalSubmit,
                externalMessages = externalMessages
            )
        }
    }
}
