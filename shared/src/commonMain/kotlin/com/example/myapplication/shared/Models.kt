package com.example.myapplication.shared

import kotlinx.serialization.Serializable

@Serializable
data class MessageData(
    val content: String,
    val time: String,
    val isFromAI: Boolean
)

@Serializable
data class ChatSession(
    val id: String,
    var name: String,
    val platform: String,
    val messages: List<MessageData>,
    val timestamp: Long = 0L
)
