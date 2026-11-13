package com.example.myapplication

import java.net.Proxy
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Refresh
import android.widget.Toast
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.speech.RecognizerIntent
import android.net.Uri
import android.provider.OpenableColumns
import android.app.Activity
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlinx.serialization.Serializable
import android.content.Context
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

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

suspend fun syncBackendConversations(context: Context) {
    val token = getAuthToken(context) ?: return
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val request = okhttp3.Request.Builder()
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
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
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
                    val request = okhttp3.Request.Builder()
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
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val request = okhttp3.Request.Builder()
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
                id = java.util.UUID.randomUUID().toString(),
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

val sharedOkHttpClient: okhttp3.OkHttpClient by lazy {
    okhttp3.OkHttpClient.Builder()
        .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(75, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .proxy(java.net.Proxy.NO_PROXY)
        .dns(okhttp3.Dns.SYSTEM)
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatStructure(
    nav: Color = Color.Blue,
    platformName: String = "Unknown",
    searchWord: String = "",
    onSearchWordChange: (String) -> Unit = {},
    isGlobalSearch: Boolean = false,
    onGlobalSubmit: (String) -> Unit = {},

    externalMessages: SnapshotStateList<MessageData>? = null,
    externalIsLoading: Boolean = false
) {
    val context = LocalContext.current
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to $platformName...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val extractedText = inputStream?.bufferedReader()?.use { r -> r.readText() } ?: ""
                    if (extractedText.isNotBlank()) {
                        var fileName = "Document"
                        context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex != -1) fileName = cursor.getString(nameIndex)
                        }

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val fullPrompt = "📄 [Attached Document: $fileName]\n\n$extractedText\n\nPlease analyze this document thoroughly and provide key summary insights."
                            val currentTime = getCurrentTime()
                            val userMsg = MessageData("📄 [Document: $fileName]", currentTime, false)
                            messages.add(userMsg)

                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val historyWithDoc = messages.dropLast(1) + MessageData(fullPrompt, currentTime, false)
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
                                    messages.add(MessageData("ERROR: ${e.localizedMessage}", getCurrentTime(), true))
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Document appears empty or unreadable.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Failed to read document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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

    val onSaveSession = {
        if (messages.size > 0) {
            val session = ChatSession(
                id = java.util.UUID.randomUUID().toString(),
                name = "$platformName - ${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())}",
                platform = platformName,
                messages = messages.toList()
            )
            saveChatSession(context, session)
        }
    }

    LaunchedEffect(Unit) {
        if (messages.isNotEmpty()) {
            val targetIndex = if (isLoading) messages.size + 1 else messages.size
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty() || isLoading) {
            val targetIndex = if (isLoading) messages.size + 1 else messages.size
            listState.animateScrollToItem(targetIndex)
        }
    }

    val triggerJudgeAction: (String) -> Unit = { actionType ->
        val responses = getLatestAiResponses(context)
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
            .background(Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: AI Model Name
            Text(
                text = if (platformName == "Judge AI") "JUDGE AI" else platformName.uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = nav
            )

            // RIGHT: Backup Chat Button & Filter Icon aligned on the right!
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = {
                        if (messages.isNotEmpty()) {
                            val session = ChatSession(
                                id = java.util.UUID.randomUUID().toString(),
                                name = "$platformName Backup - ${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())}",
                                platform = platformName,
                                messages = messages.toList()
                            )
                            saveChatSession(context, session)
                            Toast.makeText(context, "Full conversation backed up to cloud & history!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No messages to backup", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = nav.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, nav.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Backup Entire Chat",
                            tint = if (nav == Color(0xFF05040E)) Color(0xFF2C2C3E) else nav,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Backup Chat ☁️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (nav == Color(0xFF05040E)) Color(0xFF2C2C3E) else nav
                        )
                    }
                }

                IconButton(
                    onClick = { isKeywordSearchActive = !isKeywordSearchActive },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isKeywordSearchActive) Icons.Default.Close else Icons.Default.FilterList,
                        contentDescription = "Filter Messages",
                        tint = nav,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (isKeywordSearchActive) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                OutlinedTextField(
                    value = inChatKeyword,
                    onValueChange = { inChatKeyword = it },
                    placeholder = { Text("Filter messages by keyword...") },
                    singleLine = true,
                    trailingIcon = {
                        if (inChatKeyword.isNotEmpty()) {
                            IconButton(onClick = { inChatKeyword = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = nav,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        if (platformName == "Judge AI") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { triggerJudgeAction("Compare") },
                        colors = ButtonDefaults.buttonColors(containerColor = nav),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚖️Compare", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { triggerJudgeAction("Contrast") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔍Contrast", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { triggerJudgeAction("Merge") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔀Merge", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { triggerJudgeAction("Tabulate") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📊Tabulate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
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
                            Toast.makeText(context, "Reload failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                items(filteredMessages) { message ->
                    val isError = message.content.startsWith("ERROR:")
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                                    color = Color.Gray,
                                    modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                                )
                            }

                            val clipboardManager = LocalClipboardManager.current
                            Surface(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            clipboardManager.setText(AnnotatedString(message.content))
                                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    ),
                                color = Color.Transparent,
                                shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (message.isFromAI) 4.dp else 20.dp,
                                    bottomEnd = if (message.isFromAI) 20.dp else 4.dp
                                ),
                                shadowElevation = 4.dp,
                                border = when {
                                    isError -> BorderStroke(1.5.dp, Color(0xFFD32F2F))
                                    message.isFromAI -> BorderStroke(1.5.dp, nav.copy(alpha = 0.5f))
                                    else -> BorderStroke(1.5.dp, nav)
                                }
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        brush = when {
                                            isError -> Brush.verticalGradient(listOf(Color(0xFFFDECEA), Color(0xFFFADBD8)))
                                            message.isFromAI -> Brush.verticalGradient(listOf(Color(0xFF28283D), Color(0xFF1A1A28)))
                                            else -> Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF11111B)))
                                        }
                                    )
                                ) {
                                    Text(
                                        text = message.content,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isError) Color(0xFFD32F2F) else Color(0xFFF3F3F8),
                                        fontWeight = if (!message.isFromAI) FontWeight.Medium else FontWeight.Normal,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            if (message.isFromAI) {
                                Text(
                                    text = message.time,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = if (message.isFromAI) Arrangement.Start else Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (message.isFromAI) "AI" else "You",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (nav == Color(0xFF05040E)) Color(0xFF2C2C3E) else nav,
                                modifier = Modifier.padding(end = 6.dp)
                            )

                            val iconColor = if (nav == Color(0xFF05040E)) Color(0xFF2C2C3E) else nav

                            // 1. COPY ICON
                            val clipboardManager = LocalClipboardManager.current
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Message",
                                    modifier = Modifier.size(18.dp),
                                    tint = iconColor
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // 2. SAVE ICON
                            IconButton(
                                onClick = {
                                    val titleSnippet = if (message.content.length > 25) message.content.take(25) + "..." else message.content
                                    val singleMessageSession = ChatSession(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = "${if (message.isFromAI) "AI Note" else "Prompt"}: $titleSnippet",
                                        platform = platformName,
                                        messages = listOf(message)
                                    )
                                    saveChatSession(context, singleMessageSession)
                                    Toast.makeText(context, "Saved chat message to history!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save Message",
                                    modifier = Modifier.size(18.dp),
                                    tint = iconColor
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // 3. REFRESH ICON (ONLY ON USER REQUESTS)
                            if (!message.isFromAI) {
                                IconButton(
                                    onClick = {
                                        val index = messages.indexOf(message)
                                        val historyToResend = if (index >= 0) messages.take(index + 1).toList() else emptyList()

                                        if (historyToResend.isNotEmpty()) {
                                            isLoading = true
                                            coroutineScope.launch {
                                                try {
                                                    val response = when (platformName) {
                                                        "Grok" -> getGrokResponse(historyToResend)
                                                        "Gemini" -> getGeminiResponse(historyToResend)
                                                        "Hugging Face" -> getHuggingFaceResponse(historyToResend)
                                                        "Judge AI" -> getCloudflareJudgeResponse(historyToResend)
                                                        else -> getGeminiResponse(historyToResend)
                                                    }
                                                    messages.add(MessageData(response, getCurrentTime(), true))
                                                } catch (e: Exception) {
                                                    messages.add(MessageData("ERROR: ${e.localizedMessage}", getCurrentTime(), true))
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh / Regenerate",
                                        modifier = Modifier.size(18.dp),
                                        tint = iconColor
                                    )
                                }

                                Spacer(Modifier.width(8.dp))
                            }

                            // 4. DELETE ICON
                            IconButton(
                                onClick = {
                                    messages.remove(message)
                                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Message",
                                    modifier = Modifier.size(18.dp),
                                    tint = iconColor
                                )
                            }
                        }
                    }
                }

                if (isLoading || externalIsLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                color = Color(0xFFE8F0FE).copy(alpha = 0.9f),
                                contentColor = Color.Black,
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$platformName is thinking...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = nav
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = nav,
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
        val showDownArrow = !isScrollingUp && lastVisibleIndex < totalItems - 1 && totalItems > 1
        val showScrollButton = showUpArrow || showDownArrow
        
        androidx.compose.animation.AnimatedVisibility(
            visible = showScrollButton,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300)) { it / 2 },
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(300)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        if (showUpArrow) {
                            listState.animateScrollToItem(0)
                        } else {
                            val targetIndex = if (isLoading) messages.size + 1 else messages.size
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = nav),
                modifier = Modifier.size(44.dp)
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                OutlinedTextField(
                    value = searchWord,
                    shape = RoundedCornerShape(20.dp),
                    onValueChange = { onSearchWordChange(it) },
                    label = { Text("Type a message...") },
                    singleLine = false,
                    maxLines = 6,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = nav,
                        unfocusedIndicatorColor = nav,
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = launchVoiceInput) {
                    Icon(Icons.Default.Mic, "Voice Search", tint = nav, modifier = Modifier.size(24.dp))
                }

                IconButton(onClick = { documentPickerLauncher.launch("*/*") }) {
                    Icon(Icons.Default.UploadFile, "Upload Document", tint = nav, modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = {
                        if (searchWord.isNotBlank()) {
                            val query = searchWord
                            val time = getCurrentTime()
                            if (isGlobalSearch) {
                                onGlobalSubmit(query)
                            } else {
                                messages.add(MessageData(query, time, false))
                                onSearchWordChange("")
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val response = when (platformName) {
                                            "Gemini" -> getGeminiResponse(messages.toList())
                                            "Hugging Face" -> getHuggingFaceResponse(messages.toList())
                                            "Grok" -> getGrokResponse(messages.toList())
                                            "Judge AI" -> getCloudflareJudgeResponse(messages.toList())
                                            else -> getGeminiResponse(messages.toList())
                                        }
                                        messages.add(MessageData(response, getCurrentTime(), true))
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Search, "Search", tint = nav, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
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
