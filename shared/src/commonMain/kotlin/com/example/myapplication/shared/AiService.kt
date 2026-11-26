package com.example.myapplication.shared

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

object AiService {
    private suspend fun openAiCompatible(
        provider: String,
        endpoint: String,
        model: String,
        apiKey: String,
        prompt: String
    ): String {
        if (apiKey.isBlank()) return "$provider API key is not configured. Add it in Settings."
        return try {
            val payload = JsonObject(mapOf(
                "model" to JsonPrimitive(model),
                "messages" to JsonArray(listOf(
                    JsonObject(mapOf("role" to JsonPrimitive("system"), "content" to JsonPrimitive("You are $provider, a helpful AI assistant. Give accurate, clear answers."))),
                    JsonObject(mapOf("role" to JsonPrimitive("user"), "content" to JsonPrimitive(prompt)))
                )),
                "max_tokens" to JsonPrimitive(2048),
                "temperature" to JsonPrimitive(0.7)
            ))
            val response: HttpResponse = kmpHttpClient.post(endpoint) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val body = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val answer = Json.parseToJsonElement(body).jsonObject["choices"]
                    ?.jsonArray?.firstOrNull()?.jsonObject?.get("message")
                    ?.jsonObject?.get("content")?.jsonPrimitive?.content.orEmpty()
                if (answer.isNotBlank()) cleanAiResponseText(answer) else "$provider returned an empty response."
            } else "$provider API error (${response.status.value}): ${body.take(240)}"
        } catch (e: Exception) {
            "$provider connection error: ${e.message ?: "Unable to connect"}"
        }
    }

    suspend fun fetchGeminiAiResponse(prompt: String, apiKey: String): String {
        if (apiKey.isBlank()) return "Gemini API key is not configured. Add it in Settings."
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val payload = JsonObject(mapOf(
                "contents" to JsonArray(listOf(JsonObject(mapOf(
                    "role" to JsonPrimitive("user"),
                    "parts" to JsonArray(listOf(JsonObject(mapOf("text" to JsonPrimitive(prompt)))))
                )))),
                "generationConfig" to JsonObject(mapOf("maxOutputTokens" to JsonPrimitive(2048)))
            ))
            val response: HttpResponse = kmpHttpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val body = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val answer = Json.parseToJsonElement(body).jsonObject["candidates"]?.jsonArray
                    ?.firstOrNull()?.jsonObject?.get("content")?.jsonObject?.get("parts")
                    ?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content.orEmpty()
                if (answer.isNotBlank()) cleanAiResponseText(answer) else "Gemini returned an empty response."
            } else "Gemini API error (${response.status.value}): ${body.take(240)}"
        } catch (e: Exception) {
            "Gemini connection error: ${e.message ?: "Unable to connect"}"
        }
    }

    suspend fun fetchGrokAiResponse(prompt: String, apiKey: String): String =
        openAiCompatible("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "openai/gpt-oss-20b", apiKey, prompt)

    suspend fun fetchHuggingFaceAiResponse(prompt: String, apiKey: String): String =
        openAiCompatible("Hugging Face", "https://router.huggingface.co/v1/chat/completions", "Qwen/Qwen2.5-72B-Instruct", apiKey, prompt)

    suspend fun fetchCerebrasAiResponse(prompt: String, apiKey: String): String =
        openAiCompatible("Cerebras", "https://api.cerebras.ai/v1/chat/completions", "llama3.1-8b", apiKey, prompt)
}
