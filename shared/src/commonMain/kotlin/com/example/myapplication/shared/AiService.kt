package com.example.myapplication.shared

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

object AiService {

    suspend fun fetchGeminiAiResponse(prompt: String): String {
        return try {
            val systemPrompt = "You are Gemini, an intelligent, helpful AI assistant. Provide clear, detailed, well-formatted answers."
            val apiKey = ""
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

            val payload = JsonObject(
                mapOf(
                    "system_instruction" to JsonObject(
                        mapOf("parts" to JsonArray(listOf(JsonObject(mapOf("text" to JsonPrimitive(systemPrompt))))))
                    ),
                    "contents" to JsonArray(
                        listOf(JsonObject(mapOf("role" to JsonPrimitive("user"), "parts" to JsonArray(listOf(JsonObject(mapOf("text" to JsonPrimitive(prompt))))))))
                    ),
                    "generationConfig" to JsonObject(mapOf("maxOutputTokens" to JsonPrimitive(2048)))
                )
            )

            val response: HttpResponse = kmpHttpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            val bodyText = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val json = Json.parseToJsonElement(bodyText)
                val rawAnswer = json.jsonObject["candidates"]
                    ?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("content")
                    ?.jsonObject?.get("parts")
                    ?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.content ?: ""

                if (rawAnswer.isNotBlank()) cleanAiResponseText(rawAnswer) else "Gemini returned an empty response."
            } else {
                "Gemini API Error (${response.status.value})"
            }
        } catch (e: Exception) {
            "Gemini Connection Error: ${e.message ?: "Unable to connect"}"
        }
    }

    suspend fun fetchGrokAiResponse(prompt: String): String {
        return try {
            val apiKey = "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"
            val url = "https://api.groq.com/openai/v1/chat/completions"

            val payload = JsonObject(
                mapOf(
                    "model" to JsonPrimitive("qwen/qwen3.8-27b"),
                    "messages" to JsonArray(
                        listOf(
                            JsonObject(mapOf("role" to JsonPrimitive("system"), "content" to JsonPrimitive("You are Grok, an intelligent AI assistant."))),
                            JsonObject(mapOf("role" to JsonPrimitive("user"), "content" to JsonPrimitive(prompt)))
                        )
                    ),
                    "max_completion_tokens" to JsonPrimitive(2048)
                )
            )

            val response: HttpResponse = kmpHttpClient.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            val bodyText = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val json = Json.parseToJsonElement(bodyText)
                val rawAnswer = json.jsonObject["choices"]
                    ?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")
                    ?.jsonObject?.get("content")
                    ?.jsonPrimitive?.content ?: ""

                if (rawAnswer.isNotBlank()) cleanAiResponseText(rawAnswer) else "Grok returned an empty response."
            } else {
                "Grok API Error (${response.status.value})"
            }
        } catch (e: Exception) {
            "Grok Connection Error: ${e.message ?: "Unable to connect"}"
        }
    }

    suspend fun fetchHuggingFaceAiResponse(prompt: String): String {
        return try {
            val apiKey = "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"
            val url = "https://api.groq.com/openai/v1/chat/completions"

            val payload = JsonObject(
                mapOf(
                    "model" to JsonPrimitive("qwen/qwen3.8-27b"),
                    "messages" to JsonArray(
                        listOf(
                            JsonObject(mapOf("role" to JsonPrimitive("system"), "content" to JsonPrimitive("You are Hugging Face AI assistant."))),
                            JsonObject(mapOf("role" to JsonPrimitive("user"), "content" to JsonPrimitive(prompt)))
                        )
                    ),
                    "max_completion_tokens" to JsonPrimitive(2048)
                )
            )

            val response: HttpResponse = kmpHttpClient.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            val bodyText = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val json = Json.parseToJsonElement(bodyText)
                val rawAnswer = json.jsonObject["choices"]
                    ?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("message")
                    ?.jsonObject?.get("content")
                    ?.jsonPrimitive?.content ?: ""

                if (rawAnswer.isNotBlank()) cleanAiResponseText(rawAnswer) else "Hugging Face returned an empty response."
            } else {
                "Hugging Face API Error (${response.status.value})"
            }
        } catch (e: Exception) {
            "Hugging Face Connection Error: ${e.message ?: "Unable to connect"}"
        }
    }
}
