package com.example.myapplication

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

fun getLatestAiResponses(
    sourceMessages: Map<String, List<MessageData>>
): Map<String, String> {

    val platforms = listOf("Gemini", "Hugging Face", "Grok")
    val result = mutableMapOf<String, String>()

    for (platform in platforms) {
        val messages = sourceMessages[platform] ?: emptyList()

        // Find the latest question sent to this AI
        val lastUserIndex = messages.indexOfLast { !it.isFromAI }

        // Only accept an AI response that came after that question
        val latestResponse = messages
            .drop(lastUserIndex + 1)
            .lastOrNull {
                it.isFromAI &&
                        it.content.isNotBlank() &&
                        !it.content.trim().startsWith("ERROR:", ignoreCase = true)
            }
            ?.content
            ?.trim()

        val response = if (latestResponse.isNullOrBlank()) {
            "No valid response received from $platform for the latest search."
        } else {
            latestResponse
        }

        result[platform] = if (response.length > 4000) {
            response.take(4000) + "\n[Response shortened for evaluation]"
        } else {
            response
        }
    }

    return result
}

suspend fun getCloudflareJudgeResponse(
    history: List<MessageData>
): String = withContext(Dispatchers.IO) {

    try {
        val apiMessages = mutableListOf<JsonObject>()

        apiMessages.add(
            JsonObject(
                mapOf(
                    "role" to JsonPrimitive("system"),
                    "content" to JsonPrimitive(
                        """
                        You are Judge AI, an independent expert evaluator.

                        Your responsibility is to evaluate responses from:
                        1. Gemini
                        2. Hugging Face
                        3. Grok

                        IMPORTANT RULES:

                        - Evaluate the actual responses supplied to you.
                        - Never invent a response for a missing AI platform.
                        - Never mistake the user's question for an AI response.
                        - Identify which platform provides the strongest answer.
                        - Check factual accuracy, relevance, completeness and clarity.
                        - Identify agreements and disagreements.
                        - Point out incorrect or unsupported claims.
                        - Do not automatically favour any platform.
                        - If a response is missing, clearly state that it is missing.
                        - Provide a useful, detailed and well-organized conclusion.

                        Follow the requested operation: Compare, Contrast,
                        Tabulate or Merge.

                        Be objective, analytical and precise.
                        """.trimIndent()
                    )
                )
            )
        )

        val trimmedHistory = history
            .filter { it.content.isNotBlank() }
            .takeLast(6)

        trimmedHistory.forEach { message ->

            val role = if (message.isFromAI) "assistant" else "user"

            val content = message.content
                .trim()
                .take(5000)

            apiMessages.add(
                JsonObject(
                    mapOf(
                        "role" to JsonPrimitive(role),
                        "content" to JsonPrimitive(content)
                    )
                )
            )
        }


        val accountId = "2dd9cbbd4d8844579f4fdd1a2d067d28"
        val cfToken = ""
        val targetUrl =
            "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/@cf/meta/llama-3.1-8b-instruct"

        val payload = JsonObject(
            mapOf(
                "messages" to JsonArray(apiMessages),
                "max_tokens" to JsonPrimitive(2048)
            )
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestBody = payload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(targetUrl)
            .addHeader("Authorization", "Bearer $cfToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->

            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Cloudflare Judge AI error ${response.code}: $responseText"
                )
            }

            val json = Json.parseToJsonElement(responseText).jsonObject

            val answer = json["result"]
                ?.jsonObject
                ?.get("response")
                ?.jsonPrimitive
                ?.content

            if (answer.isNullOrBlank()) {
                throw IllegalStateException(
                    "Judge AI returned an empty response."
                )
            }

            cleanResponseText(answer)
        }

    } catch (e: Exception) {

        Log.e("JudgeAI", "Judge evaluation failed", e)

        throw IllegalStateException(
            "Judge AI could not complete the evaluation: ${e.localizedMessage}",
            e
        )
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
    sourceMessages: Map<String, List<MessageData>> = emptyMap(),
    externalMessages: SnapshotStateList<MessageData>? = null
) {
    val judgeColor = Color(0xFF8B5CF6)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7F9)
    ) {
        if (!isGlobalSearch) {
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
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = judgeColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFF3E8FF)
                        ) {
                            Text(
                                text = "ARENA MODE REQUIRED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = judgeColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "JUDGE AI IS PAUSED",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Turn on MultiSearch ⚡ in Settings to compare, contrast, merge, and tabulate AI answers side-by-side.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = judgeColor),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Enable in Settings ⚡", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
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
                judgeSourceMessages = sourceMessages,
                externalMessages = externalMessages
            )
        }
    }
}