package com.example.myapplication.shared

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val kmpHttpClient: HttpClient by lazy {
    HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
}

fun cleanAiResponseText(rawText: String): String {
    if (rawText.isBlank()) return rawText
    return rawText
        .replace(Regex("\\*\\*|\\*|`"), "")
        .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("[\\r\\t]"), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .replace(Regex("[!;:#]{2,}"), "")
        .trim()
}
