package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface YouTubeService {
    suspend fun getTranscript(videoUrl: String): String?
}

class YouTubeServiceImpl(
    private val
    backendBaseUrl: String = "https://multisearch-ai-backend.onrender.com"
) : YouTubeService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun getTranscript(videoUrl: String): String? {
        return withContext(Dispatchers.IO) {
            val videoId = extractYouTubeVideoId(videoUrl)
                ?: throw IllegalArgumentException("Invalid YouTube URL. Unable to extract Video ID.")

            // 1. Try public captions/transcript first
            val localTranscript = fetchYouTubeTranscript(videoId)
            if (!localTranscript.isNullOrBlank()) {
                return@withContext localTranscript
            }

            // 2. Fallback to Backend Transcription Service for authorized videos
            val endpoint = "$backendBaseUrl/youtube/transcribe"
            val payload = JsonObject(mapOf("url" to JsonPrimitive(videoUrl)))
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string()
                ?: throw IllegalStateException("Backend unavailable. Empty response received.")

            if (!response.isSuccessful) {
                throw IllegalStateException("Transcription service unavailable (${response.code})")
            }

            val json = Json.parseToJsonElement(responseText).jsonObject
            val isSuccess = json["success"]?.jsonPrimitive?.boolean ?: false

            if (isSuccess) {
                json["transcript"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("Backend returned empty transcript.")
            } else {
                val errorMsg = json["error"]?.jsonPrimitive?.content ?: "Transcription failed on backend."
                throw IllegalStateException(errorMsg)
            }
        }
    }
}
