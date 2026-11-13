package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val isPending: Boolean = false)

class GeminiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GeminiChatScreen()
                }
            }
        }
    }
}

@Composable
fun GeminiChatScreen() {
    var userInput by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(ChatMessage("Hello! I'm Gemini. How can I help you today?", false)) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Gemini",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = { newValue -> userInput = newValue },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter a prompt here", color = Color.Gray) },
                shape = RoundedCornerShape(28.dp),
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFDADCE0),
                    focusedBorderColor = Color(0xFF4285F4),
                    cursorColor = Color(0xFF4285F4)
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (userInput.isNotBlank()) {
                        val userText = userInput
                        messages.add(ChatMessage(userText, true))
                        userInput = ""
                        
                        coroutineScope.launch {
                            messages.add(ChatMessage("Gemini is thinkingS...", false, isPending = true))
                            delay(1500)
                            if (messages.isNotEmpty()) {
                                messages.removeAt(messages.size - 1)
                            }
                            val response = getMockResponse(userText)
                            messages.add(ChatMessage(response, false))
                        }
                    }
                },
                enabled = userInput.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF4285F4),
                    disabledContentColor = Color.Gray
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) Color(0xFFD2E3FC) else Color(0xFFF1F3F4)
    val textColor = if (message.isUser) Color(0xFF174EA6) else Color(0xFF3C4043)
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = backgroundColor,
            shape = shape,
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                color = if (message.isPending) Color.Gray else textColor,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontWeight = if (message.isPending) FontWeight.Normal else FontWeight.Medium
            )
        }
    }
}

fun getMockResponse(input: String): String {
    val lowerInput = input.lowercase()
    return when {
        lowerInput.contains("hello") || lowerInput.contains("hi") -> 
            "Hello! How can I assist you today?"
        lowerInput.contains("weather") -> 
            "I don't have real-time access to weather right now, but it looks like a great day to code!"
        lowerInput.contains("who are you") -> 
            "I'm a Gemini-like AI interface built with Jetpack Compose."
        lowerInput.contains("help") -> 
            "I can help you with simple questions or just chat. What's on your mind?"
        else -> 
            "That's an interesting point about '$input'. Tell me more!"
    }
}

