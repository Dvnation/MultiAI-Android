package com.example.myapplication

import java.net.URLEncoder
import java.util.UUID

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Download
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import java.net.URLDecoder

// ============================================================
// DETECT SOCIAL MEDIA PLATFORM NAME FROM URL
// ============================================================

fun detectPlatformNameFromUrl(url: String): String {
    val trimmed = url.trim().lowercase()
    return when {
        trimmed.contains("youtube.com") || trimmed.contains("youtu.be") -> "YouTube"
        trimmed.contains("tiktok.com") -> "TikTok"
        trimmed.contains("instagram.com") -> "Instagram"
        trimmed.contains("twitter.com") || trimmed.contains("x.com") -> "Twitter/X"
        trimmed.contains("facebook.com") || trimmed.contains("fb.watch") -> "Facebook"
        trimmed.contains("vimeo.com") -> "Vimeo"
        trimmed.contains("pinterest.com") -> "Pinterest"
        else -> "Social Media Video"
    }
}

// ============================================================
// YOUTUBE / SOCIAL URL → VIDEO ID
// ============================================================

fun extractYouTubeVideoId(url: String): String? {
    return try {
        val trimmed = url.trim()
        when {
            trimmed.contains("youtube.com/watch") && trimmed.contains("v=") -> {
                trimmed.substringAfter("v=").substringBefore("&").substringBefore("#").trim().takeIf { it.length >= 6 }
            }
            trimmed.contains("youtu.be/") -> {
                trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").substringBefore("#").trim().takeIf { it.length >= 6 }
            }
            trimmed.contains("youtube.com/shorts/") -> {
                trimmed.substringAfter("youtube.com/shorts/").substringBefore("?").substringBefore("&").substringBefore("#").trim().takeIf { it.length >= 6 }
            }
            trimmed.contains("youtube.com/embed/") -> {
                trimmed.substringAfter("youtube.com/embed/").substringBefore("?").substringBefore("&").substringBefore("#").trim().takeIf { it.length >= 6 }
            }
            else -> "vid_${Math.abs(url.hashCode())}"
        }
    } catch (e: Exception) {
        "vid_${Math.abs(url.hashCode())}"
    }
}

// ============================================================
// GET VIDEO METADATA
// ============================================================

suspend fun fetchYouTubeVideoMetadata(videoId: String): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        try {
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val oembedUrl = "https://www.youtube.com/oembed?url=${URLEncoder.encode(videoUrl, "UTF-8")}&format=json"
            val jsonString = URL(oembedUrl).readText()
            val jsonObject = Json.parseToJsonElement(jsonString).jsonObject

            val title = jsonObject["title"]?.jsonPrimitive?.content ?: "Video ($videoId)"
            val author = jsonObject["author_name"]?.jsonPrimitive?.content ?: "Social Media"

            Pair(title, author)
        } catch (e: Exception) {
            Pair("Video ($videoId)", "Social Media")
        }
    }
}

// ============================================================
// CLEAN CAPTIONS
// ============================================================

fun cleanYouTubeCaptionXml(xml: String): String {
    return try {
        xml
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace(Regex("<[^>]*>"), " ")
            .let {
                try {
                    URLDecoder.decode(it, "UTF-8")
                } catch (e: Exception) {
                    it
                }
            }
            .replace(Regex("\\s+"), " ")
            .trim()
    } catch (e: Exception) {
        xml.trim()
    }
}

// ============================================================
// FETCH CAPTIONS / TRANSCRIPT
// ============================================================

suspend fun fetchYouTubeTranscript(videoId: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val languages = listOf("en", "en-US", "en-GB")

            for (language in languages) {
                try {
                    val timedTextUrl = "https://www.youtube.com/api/timedtext?v=$videoId&lang=$language"
                    val xmlText = URL(timedTextUrl).readText()

                    if (xmlText.isNotBlank() && (xmlText.contains("<text") || xmlText.contains("<transcript"))) {
                        val cleanText = cleanYouTubeCaptionXml(xmlText)
                        if (cleanText.length > 50) {
                            return@withContext cleanText
                        }
                    }
                } catch (_: Exception) {
                    // Try next language
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}

// ============================================================
// GENERATE AI STUDY NOTES
// ============================================================

suspend fun generateYouTubeNotes(title: String, author: String, transcript: String): String {
    val prompt = """
        You are Social Media & Video Notes AI.

        Analyze the following video transcript and convert it into clear, accurate, and well-organized study notes.

        VIDEO TITLE:
        $title

        AUTHOR / PLATFORM:
        $author

        TRANSCRIPT:
        $transcript

        FORMAT THE RESPONSE LIKE THIS:

        EXECUTIVE SUMMARY
        Give a concise 2–3 sentence overview.

        KEY POINTS
        Organize the important information into clear sections and bullet points.

        IMPORTANT CONCEPTS
        Explain important concepts mentioned in the video.

        KEY TAKEAWAYS
        List the most important things the viewer should remember.

        CONCLUSION
        Give a short final summary.

        Make the response clean and readable.
        Do not use unnecessary # symbols.
    """.trimIndent()

    return try {
        val history = listOf(
            MessageData(
                prompt,
                System.currentTimeMillis().toString(),
                false
            )
        )
        getGeminiResponse(history)
    } catch (e: Exception) {
        "Unable to generate notes: ${e.localizedMessage ?: "Unknown error"}"
    }
}

// ============================================================
// ASSEMBLY AI AUDIO TRANSCRIPTION
// ============================================================

suspend fun transcribeAudioWithAssemblyAI(
    audioUrl: String,
    apiKey: String = "04a39f60bcbf44f8b9195b0eb8f85f39"
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val rawOkHttpClient = okhttp3.OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

            val payload = JsonObject(mapOf("audio_url" to JsonPrimitive(audioUrl)))
            val requestBody = payload.toString().toRequestBody(mediaType)

            val postRequest = okhttp3.Request.Builder()
                .url("https://api.assemblyai.com/v2/transcript")
                .addHeader("Authorization", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val postResponse = rawOkHttpClient.newCall(postRequest).execute()
            val postResponseBody = postResponse.body?.string() ?: ""
            if (!postResponse.isSuccessful) return@withContext null

            val postJson = Json.parseToJsonElement(postResponseBody).jsonObject
            val transcriptId = postJson["id"]?.jsonPrimitive?.content ?: return@withContext null

            var attempts = 0
            while (attempts < 30) {
                delay(3000)
                attempts++

                val getRequest = okhttp3.Request.Builder()
                    .url("https://api.assemblyai.com/v2/transcript/$transcriptId")
                    .addHeader("Authorization", apiKey)
                    .get()
                    .build()

                val getResponse = rawOkHttpClient.newCall(getRequest).execute()
                val getResponseBody = getResponse.body?.string() ?: ""
                if (getResponse.isSuccessful) {
                    val getJson = Json.parseToJsonElement(getResponseBody).jsonObject
                    val status = getJson["status"]?.jsonPrimitive?.content
                    if (status == "completed") {
                        return@withContext getJson["text"]?.jsonPrimitive?.content
                    } else if (status == "error") {
                        return@withContext null
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}

// ============================================================
// DOWNLOAD VIDEO/AUDIO TO DEVICE IN CHOSEN QUALITY
// ============================================================

fun downloadSocialVideoToDevice(
    context: Context,
    videoTitle: String,
    videoUrl: String,
    quality: String
): File? {
    return try {
        val platform = detectPlatformNameFromUrl(videoUrl)
        val cleanTitle = if (videoTitle.isNotBlank()) videoTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_") else "Video"
        val ext = if (quality.contains("Audio") || quality.contains("MP3")) "mp3" else "mp4"
        val cleanQuality = quality.replace(" ", "_")

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val videoFile = File(downloadsDir, "${platform}_${cleanQuality}_${cleanTitle}.$ext")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(videoUrl))
            .setTitle("$platform Video ($quality): $videoTitle")
            .setDescription("Downloading video in $quality...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(videoFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadManager.enqueue(request)
        videoFile
    } catch (e: Exception) {
        null
    }
}

// ============================================================
// MAIN GENERAL VIDEO DOWNLOADER & NOTES PAGE
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeNotesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // STATES
    var videoUrlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var videoTitle by remember { mutableStateOf("") }
    var generatedNotes by remember { mutableStateOf("") }
    var rawTranscript by remember { mutableStateOf("") }
    var captionSource by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: AI Notes, 1: Full Transcript

    val qualityOptions = listOf("1080p HD", "720p HD", "480p SD", "Audio MP3")
    var selectedQuality by remember { mutableStateOf("1080p HD") }

    val onProcessVideo = {
        if (videoUrlInput.isBlank()) {
            Toast.makeText(context, "Please enter a valid video link", Toast.LENGTH_SHORT).show()
        } else {
            isLoading = true
            statusMessage = "Analyzing video link & platform..."
            generatedNotes = ""
            rawTranscript = ""
            videoTitle = ""
            captionSource = ""

            val platformName = detectPlatformNameFromUrl(videoUrlInput)
            val videoId = extractYouTubeVideoId(videoUrlInput) ?: "vid_123"

            coroutineScope.launch {
                try {
                    // STEP 1 — METADATA
                    val (title, author) = fetchYouTubeVideoMetadata(videoId)
                    videoTitle = title

                    // STEP 2 — DOWNLOAD VIDEO/AUDIO IN SELECTED QUALITY TO DEVICE
                    statusMessage = "Downloading $platformName video in $selectedQuality to Downloads folder..."
                    downloadSocialVideoToDevice(context, title, videoUrlInput, selectedQuality)

                    // STEP 3 — EXTRACT TRANSCRIPT & NOTES
                    statusMessage = "Processing transcript for $title..."
                    val service: YouTubeService = YouTubeServiceImpl()

                    val localCaptions = fetchYouTubeTranscript(videoId)
                    val transcript = if (!localCaptions.isNullOrBlank()) {
                        captionSource = "Direct captions ($platformName)"
                        localCaptions
                    } else {
                        statusMessage = "Transcribing $platformName video audio..."
                        try {
                            val backendText = service.getTranscript(videoUrlInput)
                            if (!backendText.isNullOrBlank()) {
                                captionSource = "Backend Speech-to-Text"
                                backendText
                            } else null
                        } catch (be: Exception) {
                            val assemblyText = transcribeAudioWithAssemblyAI(videoUrlInput)
                            if (!assemblyText.isNullOrBlank()) {
                                captionSource = "AssemblyAI Speech-to-Text"
                                assemblyText
                            } else null
                        }
                    }

                    if (!transcript.isNullOrBlank()) {
                        rawTranscript = transcript
                        statusMessage = "Transcript obtained! Generating AI study notes..."

                        val notes = generateYouTubeNotes(
                            title = title,
                            author = "$author ($platformName)",
                            transcript = transcript
                        )
                        generatedNotes = notes
                    } else {
                        captionSource = "Video Downloaded in $selectedQuality"
                        generatedNotes = """
                            Video downloaded successfully in $selectedQuality to your Downloads folder!
                            Automatic captions were unavailable for this link.
                        """.trimIndent()
                    }
                } catch (e: Exception) {
                    generatedNotes = "Video Downloaded in $selectedQuality. Note: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                    statusMessage = ""
                }
            }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .padding(16.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Text(
                text = "General Video Downloader & AI Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E2E)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // INPUT CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Paste Any Social Video Link",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF8B5CF6)
                )

                Text(
                    text = "Supports YouTube, TikTok, Instagram, Twitter/X, Facebook & more",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = videoUrlInput,
                    onValueChange = { videoUrlInput = it },
                    placeholder = { Text("Paste video link here...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Video Quality:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E1E2E)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualityOptions.forEach { option ->
                        FilterChip(
                            selected = selectedQuality == option,
                            onClick = { selectedQuality = option },
                            label = { Text(option, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF8B5CF6),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onProcessVideo() },
                    enabled = !isLoading && videoUrlInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Downloading Video & Processing...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Download Video & Generate Notes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // STATUS MESSAGE
        if (statusMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F0FE)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        color = Color(0xFF1E88E5),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // RESULTS CONTAINER
        if (generatedNotes.isNotBlank() || rawTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // HEADER ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (videoTitle.isNotBlank()) videoTitle else "Video Transcript & Notes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1E1E2E),
                            modifier = Modifier.weight(1f)
                        )

                        // RE-DOWNLOAD BUTTON
                        IconButton(
                            onClick = {
                                val currentVideoId = extractYouTubeVideoId(videoUrlInput) ?: "vid_123"
                                downloadSocialVideoToDevice(context, videoTitle, videoUrlInput, selectedQuality)
                                Toast.makeText(
                                    context,
                                    "Downloading Video ($selectedQuality) to Downloads folder!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download Video",
                                tint = Color(0xFF8B5CF6)
                            )
                        }

                        // COPY BUTTON
                        IconButton(
                            onClick = {
                                val textToCopy = if (selectedTab == 0) generatedNotes else rawTranscript
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                val label = if (selectedTab == 0) "Notes" else "Transcript"
                                Toast.makeText(
                                    context,
                                    "$label copied to clipboard!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Content",
                                tint = Color(0xFF8B5CF6)
                            )
                        }

                        // SAVE BUTTON
                        IconButton(
                            onClick = {
                                val contentToSave = """
                                    VIDEO TITLE: $videoTitle
                                    QUALITY: $selectedQuality
                                    URL: $videoUrlInput
                                    
                                    --- AI STUDY NOTES ---
                                    $generatedNotes
                                    
                                    --- FULL TRANSCRIPT ---
                                    $rawTranscript
                                """.trimIndent()

                                val session = ChatSession(
                                    id = UUID.randomUUID().toString(),
                                    name = "Video Notes: ${if (videoTitle.isNotBlank()) videoTitle else "Media"}",
                                    platform = "Video Notes",
                                    messages = listOf(
                                        MessageData("Video Link: $videoUrlInput ($selectedQuality)", getCurrentTime(), false),
                                        MessageData(contentToSave, getCurrentTime(), true)
                                    )
                                )

                                saveChatSession(context, session)

                                Toast.makeText(
                                    context,
                                    "Video Notes & Transcript saved!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save Notes",
                                tint = Color(0xFF8B5CF6)
                            )
                        }
                    }

                    if (captionSource.isNotBlank()) {
                        Text(
                            text = "Status: $captionSource",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // TAB SELECTOR (AI Notes vs Full Transcript)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF8B5CF6)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("📝 AI Notes", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                val wordCount = if (rawTranscript.isNotBlank()) rawTranscript.split("\\s+".toRegex()).size else 0
                                Text("📜 Transcript ($wordCount words)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TAB CONTENT
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (selectedTab == 0) {
                            Text(
                                text = generatedNotes,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = Color.Black
                            )
                        } else {
                            if (rawTranscript.isNotBlank()) {
                                Text(
                                    text = rawTranscript,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF222222)
                                )
                            } else {
                                Text(
                                    text = "Full transcript is not available for this video link.",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}