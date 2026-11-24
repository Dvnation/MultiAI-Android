package com.example.myapplication

import android.app.Activity
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Send
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.net.Proxy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
    val timestamp: Long = System.currentTimeMillis()
)

fun saveAuthCredentials(context: Context, token: String, refreshToken: String, email: String) {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("auth_token", token)
        .putString("refresh_token", refreshToken)
        .putString("user_email", email)
        .apply()
}

fun getAuthToken(context: Context): String? {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    return prefs.getString("auth_token", null)
}

fun getUserEmail(context: Context): String? {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    return prefs.getString("user_email", null)
}

fun clearAuthCredentials(context: Context) {
    val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE).edit().clear().apply()
    context.getSharedPreferences("active_chats", Context.MODE_PRIVATE).edit().clear().apply()
}

suspend fun performBackendLogin(context: Context, emailVal: String, passwordVal: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JsonObject(
                mapOf(
                    "email" to JsonPrimitive(emailVal.trim()),
                    "password" to JsonPrimitive(passwordVal.trim())
                )
            ).toString()

            val request = Request.Builder()
                .url("https://multisearch-ai-backend.onrender.com/auth/login")
                .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            sharedOkHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val jsonObj = Json.parseToJsonElement(bodyStr).jsonObject
                    val token = jsonObj["accessToken"]?.jsonPrimitive?.content ?: jsonObj["token"]?.jsonPrimitive?.content ?: ""
                    val refreshToken = jsonObj["refreshToken"]?.jsonPrimitive?.content ?: ""
                    if (token.isNotBlank()) {
                        saveAuthCredentials(context, token, refreshToken, emailVal.trim())
                        Pair(true, "Login successful!")
                    } else {
                        Pair(false, "Token missing in response")
                    }
                } else {
                    Pair(false, "Login failed (${response.code})")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message ?: "Unable to connect"}")
        }
    }
}

suspend fun performBackendRegister(context: Context, nameVal: String, emailVal: String, passwordVal: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JsonObject(
                mapOf(
                    "name" to JsonPrimitive(nameVal.trim()),
                    "email" to JsonPrimitive(emailVal.trim()),
                    "password" to JsonPrimitive(passwordVal.trim())
                )
            ).toString()

            val request = Request.Builder()
                .url("https://multisearch-ai-backend.onrender.com/auth/register")
                .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            sharedOkHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val jsonObj = Json.parseToJsonElement(bodyStr).jsonObject
                    val token = jsonObj["accessToken"]?.jsonPrimitive?.content ?: jsonObj["token"]?.jsonPrimitive?.content ?: ""
                    val refreshToken = jsonObj["refreshToken"]?.jsonPrimitive?.content ?: ""
                    if (token.isNotBlank()) {
                        saveAuthCredentials(context, token, refreshToken, emailVal.trim())
                        Pair(true, "Registration successful!")
                    } else {
                        Pair(false, "Registered successfully! Please sign in.")                    }
                } else {
                    Pair(false, "Registration failed (${response.code})")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message ?: "Unable to connect"}")
        }
    }
}


@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val brandGreen = Color(0xFF00B86B)
    val darkText = Color(0xFF172B24)
    val mutedText = Color(0xFF718078)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F8F7),
                        Color(0xFFEAF5EF),
                        Color(0xFFF5F8F7)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = brandGreen.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⚡", fontSize = 30.sp)
                    }
                }

                Text(
                    text = if (isRegisterMode)
                        "Create MultiAI Account"
                    else
                        "Welcome to MultiAI",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = darkText
                )

                Text(
                    text = if (isRegisterMode)
                        "Sign up to sync your chats across all devices"
                    else
                        "Sign in to access your multi-model AI dashboard",
                    fontSize = 13.sp,
                    color = mutedText,
                    textAlign = TextAlign.Center
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText,
                            focusedLabelColor = brandGreen,
                            unfocusedLabelColor = mutedText,
                            focusedBorderColor = brandGreen,
                            unfocusedBorderColor = Color(0xFFE0E8E3),
                            cursorColor = brandGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = darkText,
                        unfocusedTextColor = darkText,
                        focusedLabelColor = brandGreen,
                        unfocusedLabelColor = mutedText,
                        focusedBorderColor = brandGreen,
                        unfocusedBorderColor = Color(0xFFE0E8E3),
                        cursorColor = brandGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = darkText,
                        unfocusedTextColor = darkText,
                        focusedLabelColor = brandGreen,
                        unfocusedLabelColor = mutedText,
                        focusedBorderColor = brandGreen,
                        unfocusedBorderColor = Color(0xFFE0E8E3),
                        cursorColor = brandGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        color = if (
                            statusMessage.contains("successful", ignoreCase = true) ||
                            statusMessage.contains("registered", ignoreCase = true)
                        ) brandGreen else Color(0xFFE53935),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                            statusMessage = "Please enter email and password"
                            return@Button
                        }

                        if (isRegisterMode && nameInput.isBlank()) {
                            statusMessage = "Please enter your full name"
                            return@Button
                        }

                        haptics.performHapticFeedback(
                            HapticFeedbackType.TextHandleMove
                        )

                        isLoading = true
                        statusMessage = ""

                        coroutineScope.launch {
                            val res = if (isRegisterMode) {
                                performBackendRegister(
                                    context,
                                    nameInput,
                                    emailInput,
                                    passwordInput
                                )
                            } else {
                                performBackendLogin(
                                    context,
                                    emailInput,
                                    passwordInput
                                )
                            }

                            isLoading = false
                            statusMessage = res.second

                            if (res.first) {
                                delay(600)
                                onAuthSuccess()
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandGreen,
                        disabledContainerColor = brandGreen.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode)
                                "Create Account"
                            else
                                "Sign In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        statusMessage = ""
                    }
                ) {
                    Text(
                        text = if (isRegisterMode)
                            "Already have an account? Sign In"
                        else
                            "Don't have an account? Sign Up",
                        color = brandGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

suspend fun syncBackendConversations(context: Context) {
    val token = getAuthToken(context) ?: return
    withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://multisearch-ai-backend.onrender.com/api/conversations")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            sharedOkHttpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string() ?: ""
                if (response.isSuccessful && responseText.isNotBlank()) {
                    val jsonObj = Json.parseToJsonElement(responseText).jsonObject
                    val isSuccess = jsonObj["success"]?.jsonPrimitive?.boolean ?: false
                    if (isSuccess) {
                        val conversationsArray = jsonObj["conversations"]?.jsonArray
                        if (conversationsArray != null) {
                            val remoteSessions = mutableListOf<ChatSession>()
                            for (element in conversationsArray) {
                                val convObj = element.jsonObject
                                val id = convObj["id"]?.jsonPrimitive?.content ?: ""
                                val name = convObj["name"]?.jsonPrimitive?.content ?: "Saved Chat"
                                val platform = convObj["platform"]?.jsonPrimitive?.content ?: "Gemini"
                                val msgList = mutableListOf<MessageData>()
                                val msgsArray = convObj["messages"]?.jsonArray
                                if (msgsArray != null) {
                                    for (msgElem in msgsArray) {
                                        val mObj = msgElem.jsonObject
                                        val content = mObj["content"]?.jsonPrimitive?.content ?: ""
                                        val time = mObj["time"]?.jsonPrimitive?.content ?: ""
                                        val isFromAI = mObj["isFromAI"]?.jsonPrimitive?.boolean ?: false
                                        msgList.add(MessageData(content, time, isFromAI))
                                    }
                                }
                                remoteSessions.add(ChatSession(id, name, platform, msgList))
                            }

                            if (remoteSessions.isNotEmpty()) {
                                val prefs = context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
                                val jsonString = Json.encodeToString(remoteSessions)
                                prefs.edit().putString("sessions_list", jsonString).apply()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun saveChatSession(context: Context, session: ChatSession) {
    try {
        val prefs = context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
        val sessions = getChatSessions(context).toMutableList()
        sessions.removeAll { it.id == session.id }
        sessions.add(0, session)
        val jsonString = Json.encodeToString(sessions)
        prefs.edit().putString("sessions_list", jsonString).apply()
        Toast.makeText(context, "Session saved to history", Toast.LENGTH_SHORT).show()

        val token = getAuthToken(context)
        if (!token.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val payload = JsonObject(
                        mapOf(
                            "sessionName" to JsonPrimitive(session.name),
                            "platform" to JsonPrimitive(session.platform),
                            "messages" to Json.encodeToJsonElement(session.messages)
                        )
                    )
                    val body = payload.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://multisearch-ai-backend.onrender.com/api/conversations")
                        .addHeader("Authorization", "Bearer $token")
                        .post(body)
                        .build()

                    sharedOkHttpClient.newCall(request).execute().close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

fun getChatSessions(context: Context): List<ChatSession> {
    val prefs = context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("sessions_list", null) ?: return emptyList()
    return try {
        Json.decodeFromString<List<ChatSession>>(jsonStr)
    } catch (e: Exception) {
        emptyList()
    }
}

fun deleteChatSession(context: Context, sessionId: String) {
    val prefs = context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
    val sessions = getChatSessions(context).toMutableList()
    sessions.removeAll { it.id == sessionId }
    prefs.edit().putString("sessions_list", Json.encodeToString(sessions)).apply()

    val token = getAuthToken(context)
    if (!token.isNullOrBlank()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://multisearch-ai-backend.onrender.com/api/conversations/$sessionId")
                    .addHeader("Authorization", "Bearer $token")
                    .delete()
                    .build()

                sharedOkHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun updateChatSessionName(context: Context, sessionId: String, newName: String) {
    val prefs = context.getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
    val sessions = getChatSessions(context).map {
        if (it.id == sessionId) it.copy(name = newName) else it
    }
    prefs.edit().putString("sessions_list", Json.encodeToString(sessions)).apply()
}

fun backupAllChatsAtOnce(context: Context) {
    val platforms = listOf("Gemini", "Hugging Face", "Grok", "Judge AI")
    var totalBackedUp = 0

    platforms.forEach { platform ->
        val activeMsgs = getActiveChat(context, platform)
        if (activeMsgs.isNotEmpty()) {
            val session = ChatSession(
                id = UUID.randomUUID().toString(),
                name = "$platform Backup - ${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())}",
                platform = platform,
                messages = activeMsgs
            )
            saveChatSession(context, session)
            totalBackedUp++
        }
    }

    if (totalBackedUp > 0) {
        Toast.makeText(context, "Successfully backed up all active AI chats to cloud & history!", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "No active chat messages to backup", Toast.LENGTH_SHORT).show()
    }
}

fun saveActiveChat(context: Context, platform: String, messages: List<MessageData>) {
    try {
        val prefs = context.getSharedPreferences("active_chats", Context.MODE_PRIVATE)
        val jsonString = Json.encodeToString(messages)
        prefs.edit().putString("active_$platform", jsonString).apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getActiveChat(context: Context, platform: String): List<MessageData> {
    val prefs = context.getSharedPreferences("active_chats", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("active_$platform", null) ?: return emptyList()
    return try {
        Json.decodeFromString<List<MessageData>>(jsonStr)
    } catch (e: Exception) {
        emptyList()
    }
}

val sharedOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .proxy(Proxy.NO_PROXY)
        .dns(Dns.SYSTEM)
        .build()
}

val chatClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}

fun cleanResponseText(rawText: String): String {
    if (rawText.isBlank()) return rawText
    return rawText
        .replace(Regex("\\*\\*|\\*|`"), "")
        .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("[\\r\\t]"), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .replace(Regex("[!;:#]{2,}"), "")
        .trim()
}

fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

fun highlightText(fullText: String, query: String, highlightColor: Color = Color(0xFFFFE066)): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(fullText)

    return buildAnnotatedString {
        var startIndex = 0
        val lowerText = fullText.lowercase(Locale.getDefault())
        val lowerQuery = query.lowercase(Locale.getDefault())

        while (startIndex < fullText.length) {
            val foundIndex = lowerText.indexOf(lowerQuery, startIndex)
            if (foundIndex == -1) {
                append(fullText.substring(startIndex))
                break
            } else {
                append(fullText.substring(startIndex, foundIndex))
                withStyle(style = SpanStyle(background = highlightColor, color = Color.Black, fontWeight = FontWeight.Bold)) {
                    append(fullText.substring(foundIndex, foundIndex + query.length))
                }
                startIndex = foundIndex + query.length
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatStructure(
    nav: Color = Color(0xFF00B86B),
    platformName: String = "Unknown",
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    isGlobalSearch: Boolean = false,
    onGlobalSubmit: (String) -> Unit = {},
    judgeSourceMessages: Map<String, List<MessageData>> = emptyMap(),
    externalMessages: SnapshotStateList<MessageData>? = null,
    externalIsLoading: Boolean = false
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val pageBgColor = Color(0xFFF5F7F9)

    val accent = nav
    val accentSoft = nav.copy(alpha = 0.10f)


    val messages = externalMessages ?: remember {
        val active = getActiveChat(context, platformName)
        mutableStateListOf<MessageData>().apply { addAll(active) }
    }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val updated = if (searchWord.isBlank()) spokenText else "$searchWord $spokenText"
                onSearchWordChange(updated)
            }
        }
    }

    val launchVoiceInput = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to $platformName...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Speech recognition not supported on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val extractedText =
                        inputStream?.bufferedReader()?.use { r -> r.readText() } ?: ""
                    if (extractedText.isNotBlank()) {
                        var fileName = "Document"
                        context.contentResolver.query(selectedUri, null, null, null, null)
                            ?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (cursor.moveToFirst() && nameIndex != -1) fileName =
                                    cursor.getString(nameIndex)
                            }

                        withContext(Dispatchers.Main) {
                            val fullPrompt =
                                "📄 [Attached Document: $fileName]\n\n$extractedText\n\nPlease analyze this document thoroughly and provide key summary insights."
                            val currentTime = getCurrentTime()
                            val userMsg = MessageData("📄 [Document: $fileName]", currentTime, false)
                            messages.add(userMsg)

                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val historyWithDoc = messages.dropLast(1) + MessageData(
                                        fullPrompt,
                                        currentTime,
                                        false
                                    )
                                    val response = when (platformName) {
                                        "Grok" -> getGrokResponse(historyWithDoc)
                                        "Gemini" -> getGeminiResponse(historyWithDoc)
                                        "Hugging Face" -> getHuggingFaceResponse(historyWithDoc)
                                        "Judge AI" -> getCloudflareJudgeResponse(historyWithDoc)
                                        else -> getGeminiResponse(historyWithDoc)
                                    }
                                    messages.add(MessageData(response, getCurrentTime(), true))
                                    onSearchWordChange("")
                                } catch (e: Exception) {
                                    messages.add(
                                        MessageData(
                                            "ERROR: ${e.localizedMessage ?: "Unable to get a response. Please try again."}",
                                            getCurrentTime(),
                                            true
                                        )
                                    )
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Document appears empty or unreadable.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to read document: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    var isKeywordSearchActive by remember { mutableStateOf(false) }
    var inChatKeyword by remember { mutableStateOf("") }

    val filteredMessages = remember(messages.toList(), inChatKeyword) {
        if (inChatKeyword.isBlank()) messages.toList()
        else messages.filter { it.content.contains(inChatKeyword, ignoreCase = true) }
    }

    LaunchedEffect(messages.size) {
        saveActiveChat(context, platformName, messages.toList())
    }

    LaunchedEffect(Unit) {
        if (messages.isNotEmpty()) {
            val targetIndex =
                if (isLoading || externalIsLoading) messages.size else (messages.size - 1).coerceAtLeast(
                    0
                )
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(messages.size, isLoading, externalIsLoading) {
        if (messages.isNotEmpty() || isLoading || externalIsLoading) {
            val targetIndex =
                if (isLoading || externalIsLoading) messages.size else (messages.size - 1).coerceAtLeast(
                    0
                )
            listState.animateScrollToItem(targetIndex)
        }
    }

    val triggerJudgeAction: (String) -> Unit = { actionType ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val responses = getLatestAiResponses(judgeSourceMessages)
        val promptText = when (actionType) {
            "Compare" -> """
                ⚖️ COMPARE RESPONSES:
                Please compare and evaluate the responses from Gemini, Hugging Face, and Grok. Highlight similarities, quality, and accuracy:

                --- Gemini ---
                ${responses["Gemini"]}

                --- Hugging Face ---
                ${responses["Hugging Face"]}

                --- Grok ---
                ${responses["Grok"]}
            """.trimIndent()

            "Contrast" -> """
                🔍 CONTRAST RESPONSES:
                Please contrast the responses from Gemini, Hugging Face, and Grok. Highlight differences, unique insights, and any conflicting information:

                --- Gemini ---
                ${responses["Gemini"]}

                --- Hugging Face ---
                ${responses["Hugging Face"]}

                --- Grok ---
                ${responses["Grok"]}
            """.trimIndent()

            "Tabulate" -> """
                📊 TABULATE RESPONSES:
                Please organize and compare the responses from Gemini, Hugging Face, and Grok into a clean, structured Markdown table with columns for AI Model, Key Points, Pros/Cons, and Summary:

                --- Gemini ---
                ${responses["Gemini"]}

                --- Hugging Face ---
                ${responses["Hugging Face"]}

                --- Grok ---
                ${responses["Grok"]}
            """.trimIndent()

            else -> """
                🔀 MERGE RESPONSES:
                Please synthesize and merge the best elements of the responses from Gemini, Hugging Face, and Grok into a single, comprehensive, high-quality answer:

                --- Gemini ---
                ${responses["Gemini"]}

                --- Hugging Face ---
                ${responses["Hugging Face"]}

                --- Grok ---
                ${responses["Grok"]}
            """.trimIndent()
        }

        val currentTime = getCurrentTime()
        messages.add(MessageData(promptText, currentTime, false))
        isLoading = true
        coroutineScope.launch {
            try {
                val result = getCloudflareJudgeResponse(messages.toList())
                messages.add(MessageData(result, getCurrentTime(), true))
            } catch (e: Exception) {
                messages.add(MessageData("ERROR: ${e.localizedMessage}", getCurrentTime(), true))
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F7F9))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AI NAME
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentSoft,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = platformName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF172B24),
                        maxLines = 1
                    )
                }
            }

            // FILTER SEARCH BAR
            OutlinedTextField(
                value = inChatKeyword,
                onValueChange = { inChatKeyword = it },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = "Filter messages...",
                        fontSize = 13.sp,
                        color = Color(0xFF9AA5AE)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Filter messages",
                        tint = accent,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (inChatKeyword.isNotEmpty()) {
                        IconButton(onClick = { inChatKeyword = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear filter",
                                tint = Color(0xFF87929E)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = accent.copy(alpha = 0.55f),
                    unfocusedBorderColor = Color(0xFFE7ECEF),
                    cursorColor = accent,
                    focusedTextColor = Color(0xFF172B24),
                    unfocusedTextColor = Color(0xFF172B24)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }


        if (platformName == "Judge AI") {

            var showJudgeActions by remember {
                mutableStateOf(false)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {

                Button(
                    onClick = {
                        showJudgeActions = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    )
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Choose Your Action",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Text(
                                text = "Compare • Contrast • Merge • Tabulate",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Show Judge AI actions",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showJudgeActions,
                    onDismissRequest = {
                        showJudgeActions = false
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .background(Color.White)
                ) {

                    DropdownMenuItem(
                        text = {
                            Text(
                                "⚖️ Compare Responses",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            showJudgeActions = false
                            triggerJudgeAction("Compare")
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                "🔍 Contrast Responses",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            showJudgeActions = false
                            triggerJudgeAction("Contrast")
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                "🔀 Merge Responses",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            showJudgeActions = false
                            triggerJudgeAction("Merge")
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                "📊 Tabulate Responses",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            showJudgeActions = false
                            triggerJudgeAction("Tabulate")
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        try {
                            val lastUserIndex = messages.indexOfLast { !it.isFromAI }
                            if (lastUserIndex >= 0) {
                                val historyToResend = messages.take(lastUserIndex + 1).toList()
                                isLoading = true
                                val response = when (platformName) {
                                    "Gemini" -> getGeminiResponse(historyToResend)
                                    "Hugging Face" -> getHuggingFaceResponse(historyToResend)
                                    "Grok" -> getGrokResponse(historyToResend)
                                    "Judge AI" -> getCloudflareJudgeResponse(historyToResend)
                                    else -> getGeminiResponse(historyToResend)
                                }
                                messages.add(MessageData(response, getCurrentTime(), true))
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Reload failed: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isLoading = false
                            isRefreshing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredMessages) { message ->
                        val isError = message.content.startsWith("ERROR:")
                        val clipboardManager = LocalClipboardManager.current

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalAlignment = if (message.isFromAI) Alignment.Start else Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.isFromAI) Arrangement.Start else Arrangement.End
                            ) {
                                if (!message.isFromAI) {
                                    Text(
                                        text = message.time,
                                        fontSize = 10.sp,
                                        color = Color(0xFF9CA3AF),
                                        modifier = Modifier.padding(end = 6.dp, bottom = 4.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .widthIn(max = 320.dp)
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                clipboardManager.setText(AnnotatedString(message.content))
                                                Toast.makeText(
                                                    context,
                                                    "Copied to clipboard!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        ),
                                    color = when {
                                        isError -> Color(0xFFFEE2E2)
                                        message.isFromAI -> Color.White
                                        else -> accent
                                    },
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (message.isFromAI) 5.dp else 20.dp,
                                        bottomEnd = if (message.isFromAI) 20.dp else 5.dp
                                    ),
                                    shadowElevation = 2.dp,
                                    border = if (message.isFromAI && !isError) {
                                        BorderStroke(1.dp, Color(0xFFE5E7EB))
                                    } else {
                                        null
                                    }
                                ) {
                                    Text(
                                        text = highlightText(message.content, inChatKeyword),
                                        modifier = Modifier.padding(
                                            horizontal = 15.dp,
                                            vertical = 11.dp
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = when {
                                            isError -> Color(0xFFB91C1C)
                                            message.isFromAI -> Color(0xFF111827)
                                            else -> Color.White
                                        },
                                        fontWeight = if (!message.isFromAI) FontWeight.Medium else FontWeight.Normal,
                                        lineHeight = 21.sp
                                    )
                                }

                                if (message.isFromAI) {
                                    Text(
                                        text = message.time,
                                        fontSize = 10.sp,
                                        color = Color(0xFF9CA3AF),
                                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp),
                                horizontalArrangement = if (message.isFromAI) Arrangement.Start else Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (message.isFromAI) accentSoft else Color(0xFF111827).copy(
                                        alpha = 0.08f
                                    )
                                ) {
                                    Text(
                                        text = if (message.isFromAI) "✨ $platformName" else "You",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (message.isFromAI) accent else Color(0xFF374151),
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }

                                Spacer(Modifier.width(6.dp))

                                val actionTint = accent

                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        clipboardManager.setText(AnnotatedString(message.content))
                                        Toast.makeText(
                                            context,
                                            "Copied to clipboard!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Message",
                                        modifier = Modifier.size(16.dp),
                                        tint = actionTint
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val titleSnippet =
                                            if (message.content.length > 25) message.content.take(25) + "..." else message.content
                                        val singleMessageSession = ChatSession(
                                            id = UUID.randomUUID().toString(),
                                            name = "${if (message.isFromAI) "AI Note" else "Prompt"}: $titleSnippet",
                                            platform = platformName,
                                            messages = listOf(message)
                                        )
                                        saveChatSession(context, singleMessageSession)
                                        Toast.makeText(
                                            context,
                                            "Saved chat message to history!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Save Message",
                                        modifier = Modifier.size(16.dp),
                                        tint = actionTint
                                    )
                                }

                                if (!message.isFromAI) {
                                    IconButton(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val index = messages.indexOf(message)
                                            val historyToResend =
                                                if (index >= 0) messages.take(index + 1)
                                                    .toList() else emptyList()

                                            if (historyToResend.isNotEmpty()) {
                                                isLoading = true
                                                coroutineScope.launch {
                                                    try {
                                                        val response = when (platformName) {
                                                            "Grok" -> getGrokResponse(
                                                                historyToResend
                                                            )

                                                            "Gemini" -> getGeminiResponse(
                                                                historyToResend
                                                            )

                                                            "Hugging Face" -> getHuggingFaceResponse(
                                                                historyToResend
                                                            )

                                                            "Judge AI" -> getCloudflareJudgeResponse(
                                                                historyToResend
                                                            )

                                                            else -> getGeminiResponse(
                                                                historyToResend
                                                            )
                                                        }
                                                        messages.add(
                                                            MessageData(
                                                                response,
                                                                getCurrentTime(),
                                                                true
                                                            )
                                                        )
                                                    } catch (e: Exception) {
                                                        messages.add(
                                                            MessageData(
                                                                "ERROR: ${e.localizedMessage}",
                                                                getCurrentTime(),
                                                                true
                                                            )
                                                        )
                                                    } finally {
                                                        isLoading = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh / Regenerate",
                                            modifier = Modifier.size(16.dp),
                                            tint = actionTint
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        messages.remove(message)
                                        Toast.makeText(
                                            context,
                                            "Message deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Message",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFF5A5F)
                                    )
                                }
                            }
                        }
                    }

                    if (isLoading || externalIsLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 5.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 10.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$platformName",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accent
                                        )
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(15.dp),
                                            color = accent,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            var isScrollingUp by remember { mutableStateOf(false) }

            LaunchedEffect(listState) {
                var prevIndex = listState.firstVisibleItemIndex
                var prevOffset = listState.firstVisibleItemScrollOffset
                snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                    .collect { (index, offset) ->
                        if (index < prevIndex || (index == prevIndex && offset < prevOffset - 5)) {
                            isScrollingUp = true
                        } else if (index > prevIndex || (index == prevIndex && offset > prevOffset + 5)) {
                            isScrollingUp = false
                        }
                        prevIndex = index
                        prevOffset = offset
                    }
            }

            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val firstVisibleIndex = listState.firstVisibleItemIndex

            val showUpArrow = isScrollingUp && firstVisibleIndex > 0 && totalItems > 1
            val showDownArrow =
                !isScrollingUp && lastVisibleIndex < totalItems - 1 && totalItems > 1
            val showScrollButton = showUpArrow || showDownArrow

            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollButton,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)) +
                        androidx.compose.animation.slideInVertically(
                            androidx.compose.animation.core.tween(
                                250
                            )
                        ) { it / 2 },
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(250)) +
                        androidx.compose.animation.slideOutVertically(
                            androidx.compose.animation.core.tween(
                                250
                            )
                        ) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            if (showUpArrow) {
                                listState.animateScrollToItem(0)
                            } else {
                                val targetIndex =
                                    if (isLoading) messages.size + 1 else messages.size
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (showUpArrow) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (showUpArrow) "Scroll to Top" else "Scroll to Bottom",
                        tint = Color.White
                    )
                }
            }
        }


        if (platformName != "Judge AI") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = searchWord,
                            onValueChange = onSearchWordChange,
                            placeholder = {
                                Text(
                                    "Message $platformName...",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = false,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF7F8FA),
                                unfocusedContainerColor = Color(0xFFF7F8FA),
                                focusedBorderColor = accent,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                cursorColor = accent,
                                focusedTextColor = Color(0xFF111827),
                                unfocusedTextColor = Color(0xFF111827)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(6.dp))

                        IconButton(
                            onClick = { launchVoiceInput() }
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = accent
                            )
                        }

                        IconButton(
                            onClick = { documentPickerLauncher.launch("*/*") }
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = "Attach file",
                                tint = accent
                            )
                        }

                        val canSend = searchWord.isNotBlank()

                        FilledIconButton(
                            onClick = {
                                val query = searchWord.trim()
                                if (query.isNotEmpty()) {
                                    if (isGlobalSearch) {
                                        onGlobalSubmit(query)
                                    } else {
                                        messages.add(
                                            MessageData(query, getCurrentTime(), false)
                                        )
                                        onSearchWordChange("")
                                        isLoading = true

                                        coroutineScope.launch {
                                            try {
                                                val response = when (platformName) {
                                                    "Gemini" -> getGeminiResponse(messages.toList())
                                                    "Hugging Face" -> getHuggingFaceResponse(
                                                        messages.toList()
                                                    )

                                                    "Grok" -> getGrokResponse(messages.toList())
                                                    else -> getGeminiResponse(messages.toList())
                                                }

                                                messages.add(
                                                    MessageData(response, getCurrentTime(), true)
                                                )
                                            } catch (e: Exception) {
                                                messages.add(
                                                    MessageData(
                                                        "ERROR: ${e.localizedMessage}",
                                                        getCurrentTime(),
                                                        true
                                                    )
                                                )
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = canSend,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = accent,
                                disabledContainerColor = Color(0xFFE5E7EB)
                            )
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send message",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

    }
}
@Preview(showBackground = true)
@Composable
fun ChatStructurePreview() {
    MyApplicationTheme {
        ChatStructure()
    }
}