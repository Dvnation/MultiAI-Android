package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : MultiAI()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiMainApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAuthenticated by remember { mutableStateOf(getAuthToken(context) != null) }
    var hasOnboarded by remember { mutableStateOf(hasCompletedOnboarding(context)) }

    if (!isAuthenticated) {
        // Step 1: Registration and Login Screen
        AuthScreen(onAuthSuccess = {
            isAuthenticated = true
        })
    } else if (!hasOnboarded) {
        // Step 2: Interactive Onboarding Tutorial Screen
        OnboardingTutorialScreen(onFinished = {
            setOnboardingCompleted(context)
            hasOnboarded = true
        })
    } else {
        // Step 3: Main App with 4 Swipeable AI Pages
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
        var isGlobalSearch by remember { mutableStateOf(false) }
        var sharedSearchWord by remember { mutableStateOf("") }
        var isGlobalLoading by remember { mutableStateOf(false) }

        // State lists for global search results
        val geminiMessages = remember {
            val active = getActiveChat(context, "Gemini")
            mutableStateListOf<MessageData>().apply { addAll(active) }
        }
        val huggingFaceMessages = remember {
            val active = getActiveChat(context, "Hugging Face")
            mutableStateListOf<MessageData>().apply { addAll(active) }
        }
        val grokMessages = remember {
            val active = getActiveChat(context, "Grok")
            mutableStateListOf<MessageData>().apply { addAll(active) }
        }
        val judgeMessages = remember {
            val active = getActiveChat(context, "Judge AI")
            mutableStateListOf<MessageData>().apply { addAll(active) }
        }

        // Unique Color palette for each AI Page
        val platformColors = listOf(
            Color(0xFF4285F4), // Gemini: Google Blue
            Color(0xFFFFB020), // Hugging Face: Amber Gold
            Color(0xFF00B86B), // Grok: Emerald Mint
            Color(0xFF8B5CF6)  // Judge AI: Purple Violet
        )

        val activeColor = platformColors[pagerState.currentPage]

        Scaffold(
            topBar = {
                Surface(
                    color = Color(0xFF0F0F1A),
                    shadowElevation = 6.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Navigation Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    color = activeColor
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("⚡", fontSize = 18.sp)
                                    }
                                }

                                Column {
                                    Text(
                                        text = "MultiAI",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    getUserEmail(context)?.let { email ->
                                        Text(
                                            text = email,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Smooth & Noticeable MultiSearch Toggle Switch
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { isGlobalSearch = !isGlobalSearch },
                                    shape = RoundedCornerShape(50),
                                    color = if (isGlobalSearch) Color(0xFF00B86B).copy(alpha = 0.25f) else Color(0xFF1E1E2E),
                                    border = BorderStroke(1.dp, if (isGlobalSearch) Color(0xFF00B86B) else Color(0xFF374151))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isGlobalSearch) "MultiSearch ON ⚡" else "MultiSearch OFF",
                                            color = if (isGlobalSearch) Color(0xFF00B86B) else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Switch(
                                            checked = isGlobalSearch,
                                            onCheckedChange = { isGlobalSearch = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF00B86B),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF1E1E2E)
                                            ),
                                            modifier = Modifier.scale(0.7f)
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    backupAllChatsAtOnce(context)
                                    Toast.makeText(context, "All AI Chats Synced ☁️", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.CloudUpload, "Backup", tint = activeColor, modifier = Modifier.size(22.dp))
                                }

                                IconButton(onClick = {
                                    clearAuthCredentials(context)
                                    isAuthenticated = false
                                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, "Sign Out", tint = Color(0xFFFF5A5F), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Unique Color Tab Row for 4 Swipeable AI Pages
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = Color(0xFF0F0F1A),
                            contentColor = activeColor,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = activeColor,
                                    height = 3.dp
                                )
                            }
                        ) {
                            val tabs = listOf("🤖 Gemini", "🤗 Hugging Face", "🚀 Grok", "⚖️ Judge AI")
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (pagerState.currentPage == index) platformColors[index] else Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF0F0F1A)
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { page ->
                when (page) {
                    0 -> ChatStructure(
                        nav = Color(0xFF4285F4),
                        platformName = "Gemini",
                        searchWord = sharedSearchWord,
                        onSearchWordChange = { sharedSearchWord = it },
                        isGlobalSearch = isGlobalSearch,
                        onGlobalSubmit = { prompt ->
                            isGlobalLoading = true
                            coroutineScope.launch {
                                val t = getCurrentTime()
                                geminiMessages.add(MessageData(prompt, t, false))
                                huggingFaceMessages.add(MessageData(prompt, t, false))
                                grokMessages.add(MessageData(prompt, t, false))

                                val jobG = launch {
                                    val r = getGeminiResponse(geminiMessages.toList())
                                    geminiMessages.add(MessageData(r, getCurrentTime(), true))
                                }
                                val jobH = launch {
                                    val r = getHuggingFaceResponse(huggingFaceMessages.toList())
                                    huggingFaceMessages.add(MessageData(r, getCurrentTime(), true))
                                }
                                val jobX = launch {
                                    val r = getGrokResponse(grokMessages.toList())
                                    grokMessages.add(MessageData(r, getCurrentTime(), true))
                                }

                                jobG.join()
                                jobH.join()
                                jobX.join()
                                isGlobalLoading = false
                                sharedSearchWord = ""
                            }
                        },
                        externalMessages = geminiMessages,
                        externalIsLoading = isGlobalLoading
                    )

                    1 -> ChatStructure(
                        nav = Color(0xFFFFB020),
                        platformName = "Hugging Face",
                        searchWord = sharedSearchWord,
                        onSearchWordChange = { sharedSearchWord = it },
                        isGlobalSearch = isGlobalSearch,
                        onGlobalSubmit = { prompt ->
                            isGlobalLoading = true
                            coroutineScope.launch {
                                val t = getCurrentTime()
                                geminiMessages.add(MessageData(prompt, t, false))
                                huggingFaceMessages.add(MessageData(prompt, t, false))
                                grokMessages.add(MessageData(prompt, t, false))

                                val jobG = launch {
                                    val r = getGeminiResponse(geminiMessages.toList())
                                    geminiMessages.add(MessageData(r, getCurrentTime(), true))
                                }
                                val jobH = launch {
                                    val r = getHuggingFaceResponse(huggingFaceMessages.toList())
                                    huggingFaceMessages.add(MessageData(r, getCurrentTime(), true))
                                }
                                val jobX = launch {
                                    val r = getGrokResponse(grokMessages.toList())
                                    grokMessages.add(MessageData(r, getCurrentTime(), true))
                                }

                                jobG.join()
                                jobH.join()
                                jobX.join()
                                isGlobalLoading = false
                                sharedSearchWord = ""
                            }
                        },
                        externalMessages = huggingFaceMessages,
                        externalIsLoading = isGlobalLoading
                    )

                    2 -> ChatStructure(
                        nav = Color(0xFF00B86B),
                        platformName = "Grok",
                        searchWord = sharedSearchWord,
                        onSearchWordChange = { sharedSearchWord = it },
                        isGlobalSearch = isGlobalSearch,
                        onGlobalSubmit = { prompt ->
                            isGlobalLoading = true
                            coroutineScope.launch {
                                val t = getCurrentTime()
                                geminiMessages.add(MessageData(prompt, t, false))
                                huggingFaceMessages.add(MessageData(prompt, t, false))
                                grokMessages.add(MessageData(prompt, t, false))

                                val jobG = launch {
                                    val r = getGeminiResponse(geminiMessages.toList())
                                    geminiMessages.add(MessageData(r, getCurrentTime(), true))
                                }
                                val jobH = launch {
                                    val r = getHuggingFaceResponse(huggingFaceMessages.toList())
                                    huggingFaceMessages.add(MessageData(r, getCurrentTime(), true))
                                }
                                val jobX = launch {
                                    val r = getGrokResponse(grokMessages.toList())
                                    grokMessages.add(MessageData(r, getCurrentTime(), true))
                                }

                                jobG.join()
                                jobH.join()
                                jobX.join()
                                isGlobalLoading = false
                                sharedSearchWord = ""
                            }
                        },
                        externalMessages = grokMessages,
                        externalIsLoading = isGlobalLoading
                    )

                    3 -> ChatStructure(
                        nav = Color(0xFF8B5CF6),
                        platformName = "Judge AI",
                        searchWord = sharedSearchWord,
                        onSearchWordChange = { sharedSearchWord = it },
                        isGlobalSearch = false,
                        externalMessages = judgeMessages,
                        externalIsLoading = false
                    )
                }
            }
        }
    }
}
