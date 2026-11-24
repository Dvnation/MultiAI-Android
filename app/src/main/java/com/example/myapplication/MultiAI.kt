package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

open class MultiAI : ComponentActivity() {
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}
        loadInterstitial()
        loadRewarded()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0F1A)
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var showAdTrigger by remember { mutableStateOf(false) }

                    val context = LocalContext.current
                    var showOnboarding by remember {
                        mutableStateOf(!hasCompletedOnboarding(context))
                    }
                    var isAuthenticated by remember {
                        mutableStateOf(!getAuthToken(context).isNullOrBlank())
                    }

                    if (showSplash) {
                        AppSplashScreen(onTimeout = {
                            showSplash = false
                            showAdTrigger = true
                        })
                    } else if (!isAuthenticated) {
                        AuthNavigationFlow(
                            onAuthSuccess = {
                                isAuthenticated = true
                            }
                        )
                    } else if (showOnboarding) {
                        OnboardingTutorialScreen(
                            onFinished = {
                                setOnboardingCompleted(context)
                                showOnboarding = false
                            }
                        )
                    } else {
                        if (showAdTrigger) {
                            LaunchedEffect(Unit) {
                                delay(1000L)
                                showInterstitial()
                                showAdTrigger = false
                            }
                        }
                        Column {
                            Box(modifier = Modifier.weight(1f)) {
                                SwipeableApp(
                                    onSignOut = {
                                        clearAuthCredentials(context)
                                        isAuthenticated = false
                                    },
                                    onViewOnboarding = {
                                        showOnboarding = true
                                    }
                                )
                            }
                            BannerAdView()
                        }
                    }
                }
            }
        }
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-5945646813751373/7123135442", adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        }
    }

    fun showRewarded() {
        if (mRewardedAd != null) {
            mRewardedAd?.show(this) {
                loadRewarded()
            }
        }
    }

    private fun loadRewarded() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-5945646813751373/6496449482", adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mRewardedAd = null
                }
            })
    }
}

@Composable
fun BannerAdView() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-5945646813751373/3075596729"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun AppSplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200L)
        onTimeout()
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0F1A),
            Color(0xFF1E1E2E),
            Color(0xFF11111B)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00B86B), Color(0xFF38BDF8)))),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("M", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "multiAI",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gemini  •  Hugging Face  •  Grok  •  Judge AI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF00B86B)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF00B86B),
                strokeWidth = 2.5.dp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableApp(onSignOut: () -> Unit = {}, onViewOnboarding: () -> Unit = {}) {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        syncBackendConversations(context)
    }

    var isGlobalSearch by remember { mutableStateOf(false) }
    var globalSearchWord by remember { mutableStateOf("") }
    val individualSearchWords = remember { mutableStateListOf("", "", "", "") }
    var checker2 by remember { mutableStateOf("") }
    var restoreTrigger by remember { mutableIntStateOf(0) }

    var fullScreenPage by remember { mutableStateOf<Int?>(null) } // 4: Settings, 5: AdsSupport, 6: History

    val onNavigate: (Int) -> Unit = { index ->
        if (index < 4) {
            fullScreenPage = null
            coroutineScope.launch { pagerState.animateScrollToPage(index) }
        } else {
            fullScreenPage = index
            coroutineScope.launch { drawerState.close() }
        }
    }

    val userEmail = getUserEmail(context) ?: "Account User"

    BackHandler {
        if (fullScreenPage != null) {
            fullScreenPage = null
        } else if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (pagerState.currentPage > 0) {
            coroutineScope.launch { pagerState.animateScrollToPage(0) }
        } else {
            (context as? Activity)?.moveTaskToBack(true)
        }
    }

    val platformColors = listOf(
        Color(0xFF4285F4), // Gemini
        Color(0xFFFF6D00), // Hugging Face
        Color(0xFF00B86B), // Grok
        Color(0xFF8B5CF6)  // Judge AI
    )

    val activeColor = platformColors[pagerState.currentPage]

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E1E2E),
                drawerContentColor = Color.White
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF28283D)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = Color(0xFF00B86B)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userEmail.take(1).uppercase(Locale.getDefault()),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "multiAI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = userEmail,
                                    fontSize = 12.sp,
                                    color = Color(0xFF00B86B)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                ListItem(
                    headlineContent = { Text("Settings", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Settings, null, tint = Color(0xFF00B86B)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onNavigate(4) }
                )
                ListItem(
                    headlineContent = { Text("Ads Support", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.VolunteerActivism, null, tint = Color(0xFF00B86B)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onNavigate(5) }
                )
                ListItem(
                    headlineContent = { Text("Chat History", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.History, null, tint = Color(0xFF00B86B)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onNavigate(6) }
                )
                ListItem(
                    headlineContent = { Text("Backup All AI Chats ☁️", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.CloudUpload, null, tint = Color(0xFF00B86B)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        coroutineScope.launch { drawerState.close() }
                        backupAllChatsAtOnce(context)
                    }
                )
                ListItem(
                    headlineContent = { Text("Onboarding Tutorial", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF00B86B)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        coroutineScope.launch { drawerState.close() }
                        onViewOnboarding()
                    }
                )
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                ListItem(
                    headlineContent = { Text("Sign Out", color = Color(0xFFFF5A5F), fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFFF5A5F)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        coroutineScope.launch { drawerState.close() }
                        onSignOut()
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        if (fullScreenPage != null) {
            when (fullScreenPage) {
                4 -> SettingsPage(
                    isGlobalSearch = isGlobalSearch,
                    onGlobalSearchChange = { isGlobalSearch = it },
                    onBack = { fullScreenPage = null }
                )
                5 -> AdsSupportPage(onBack = { fullScreenPage = null })
                6 -> ChatHistoryPage(
                    onBack = { fullScreenPage = null },
                    onRestore = { platform ->
                        restoreTrigger++
                        fullScreenPage = null
                        coroutineScope.launch {
                            val targetPage = when(platform) {
                                "Gemini" -> 0
                                "Hugging Face" -> 1
                                "Grok" -> 2
                                "Judge AI" -> 3
                                else -> 0
                            }
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                )
                7 -> RegisterPage(
                    onBack = { fullScreenPage = null },
                    onSwitchToLogin = { fullScreenPage = 8 }
                )
                8 -> SignUpPage(
                    onBack = { fullScreenPage = null },
                    onSwitchToRegister = { fullScreenPage = 7 }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF4F8F5))
                    .imePadding()
            )  {
                // Upper Navigation Header (Clean Light Theme & Well-Spaced Layout)
                Surface(
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Color(0xFF374151)
                                )
                            }

                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = "multiAI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )

                            // Generous spacing between multiAI title and MultiSearch toggle
                            Spacer(Modifier.weight(1f))

                            // Compact & Sleek MultiSearch Toggle Switch (Clean Neutral White - No Green Background)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.clickable { isGlobalSearch = !isGlobalSearch }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isGlobalSearch) "MultiSearch ON ⚡" else "MultiSearch OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isGlobalSearch) Color(0xFF111827) else Color(0xFF6B7280)
                                    )

                                    Switch(
                                        checked = isGlobalSearch,
                                        onCheckedChange = { isGlobalSearch = it },
                                        modifier = Modifier.scale(0.65f),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF111827),
                                            checkedTrackColor = Color(0xFFE5E7EB),
                                            uncheckedThumbColor = Color(0xFF9CA3AF),
                                            uncheckedTrackColor = Color(0xFFF3F4F6)
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.width(6.dp))

                            IconButton(
                                onClick = { backupAllChatsAtOnce(context) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Backup All Chats",
                                    tint = Color(0xFF00B86B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // AI Model Platform Navigation Button Bar (Unique Accent Colors & Smaller Badges)
                        // AI Model Platform Navigation Button Bar (Unique Accent Colors & Smaller Badges)

                    }
                }

                var isGeminiLoading by remember { mutableStateOf(false) }
                var isHfLoading by remember { mutableStateOf(false) }
                var isGrokLoading by remember { mutableStateOf(false) }

                val geminiMessages = remember {
                    mutableStateListOf<MessageData>().apply { addAll(getActiveChat(context, "Gemini")) }
                }
                val hfMessages = remember {
                    mutableStateListOf<MessageData>().apply { addAll(getActiveChat(context, "Hugging Face")) }
                }
                val grokMessages = remember {
                    mutableStateListOf<MessageData>().apply { addAll(getActiveChat(context, "Grok")) }
                }
                val judgeMessages = remember {
                    mutableStateListOf<MessageData>().apply { addAll(getActiveChat(context, "Judge AI")) }
                }

                val onGlobalSubmit: (String) -> Unit = { query ->
                    if (query.isNotBlank()) {
                        val time = getCurrentTime()
                        val userMsg = MessageData(query, time, false)

                        geminiMessages.add(userMsg)
                        hfMessages.add(userMsg)
                        grokMessages.add(userMsg)

                        globalSearchWord = ""
                        checker2 = ""
                        individualSearchWords[0] = ""
                        individualSearchWords[1] = ""
                        individualSearchWords[2] = ""

                        isGeminiLoading = true
                        isHfLoading = true
                        isGrokLoading = true

                        coroutineScope.launch {
                            try {
                                val resp = getGeminiResponse(geminiMessages.toList())
                                geminiMessages.add(MessageData(resp, getCurrentTime(), true))
                            } catch (e: Exception) {
                                geminiMessages.add(MessageData("ERROR: ${e.localizedMessage}", getCurrentTime(), true))
                            } finally {
                                isGeminiLoading = false
                            }
                        }

                        coroutineScope.launch {
                            try {
                                val resp = getHuggingFaceResponse(hfMessages.toList())
                                hfMessages.add(MessageData(resp, getCurrentTime(), true))
                            } catch (e: Exception) {
                                hfMessages.add(MessageData("ERROR: ${e.localizedMessage}", getCurrentTime(), true))
                            } finally {
                                isHfLoading = false
                            }
                        }

                        coroutineScope.launch {
                            try {
                                val resp = getGrokResponse(grokMessages.toList())
                                grokMessages.add(MessageData(resp, getCurrentTime(), true))
                            } catch (e: Exception) {
                                grokMessages.add(MessageData("ERROR: ${e.localizedMessage}", getCurrentTime(), true))
                            } finally {
                                isGrokLoading = false
                            }
                        }
                    }
                }



                val pageNames = listOf(
                    "✨ GEMINI",
                    "🤗 HUGGING FACE",
                    "🚀 GROK",
                    "⚖️ JUDGE AI"
                )

                val platformColors = listOf(
                    Color(0xFF4285F4), // Gemini
                    Color(0xFFFF6D00), // Hugging Face
                    Color(0xFF00B86B), // Grok
                    Color(0xFF8B5CF6)  // Judge AI
                )

                val activeColor = platformColors[pagerState.currentPage]

                Button(
                    onClick = {
                        // The button displays the currently selected page.
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        text = pageNames[pagerState.currentPage],
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }


                // 4 Swipeable AI Pages
                      HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.fillMaxSize().weight(1f),
                            beyondViewportPageCount = 3
                        ) { pageIndex ->
                            val currentSearchWord = if (pageIndex == 3) {
                                individualSearchWords[3]
                            } else {
                        if (isGlobalSearch) globalSearchWord else individualSearchWords[pageIndex]
                    }

                    val onSearchChange: (String) -> Unit = { newValue ->
                        if (pageIndex == 3) {
                            individualSearchWords[3] = newValue
                        } else {
                            if (isGlobalSearch) {
                                globalSearchWord = newValue
                                checker2 = newValue
                                individualSearchWords[0] = newValue
                                individualSearchWords[1] = newValue
                                individualSearchWords[2] = newValue
                            } else {
                                individualSearchWords[pageIndex] = newValue
                            }
                        }
                    }

                    when (pageIndex) {
                        0 -> GeminiNav(navController, currentSearchWord, onSearchChange, checker2, isGlobalSearch, onGlobalSubmit, geminiMessages, isGeminiLoading)
                        1 -> HuggingFaceNav(navController, currentSearchWord, onSearchChange, isGlobalSearch, onGlobalSubmit, hfMessages, isHfLoading)
                        2 -> GrokNav(navController, currentSearchWord, onSearchChange, isGlobalSearch, onGlobalSubmit, grokMessages, isGrokLoading)
                        3 -> JudgeAiNav(
                            navController,
                            currentSearchWord,
                            onSearchChange,
                            isGlobalSearch,
                            onOpenSettings = { fullScreenPage = 4 },
                            onGlobalSubmit = onGlobalSubmit,
                            externalMessages = judgeMessages,
                            sourceMessages = mapOf(
                                "Gemini" to geminiMessages.toList(),
                                "Hugging Face" to hfMessages.toList(),
                                "Grok" to grokMessages.toList()
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdsSupportPage(onBack: () -> Unit) {
    val context = LocalContext.current as Activity
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F1A)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Support with Ads", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { (context as? MultiAI)?.showInterstitial() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B86B)),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Watch Interstitial Ad", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val activity = context as? MultiAI
                activity?.showRewarded()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Watch Rewarded Ad", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("Support us by viewing these ads", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.weight(1f))
        BannerAdView()
    }
}

@Composable
fun SettingsPage(isGlobalSearch: Boolean, onGlobalSearchChange: (Boolean) -> Unit, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0F1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Settings ⚙️",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E2E),
                border = BorderStroke(1.dp, Color(0xFF374151)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "MultiSearch Mode ⚡",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Synchronize query typing & search across Gemini, Hugging Face, and Grok simultaneously.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                lineHeight = 18.sp
                            )
                        }

                        Switch(
                            checked = isGlobalSearch,
                            onCheckedChange = onGlobalSearchChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00B86B),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF1E1E2E)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylishNavButton(
    text: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navScale"
    )

    Surface(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color else Color(0xFFF3F4F6),
        border = BorderStroke(
            1.dp,
            if (isSelected) color else Color(0xFFE5E7EB)
        ),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            color = if (isSelected) Color.White else Color(0xFF4B5563),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChatHistoryPage(onBack: () -> Unit, onRestore: (String) -> Unit = {}) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(getChatSessions(context).sortedByDescending { it.timestamp }) }
    var editingSessionId by remember { mutableStateOf<String?>(null) }
    var editNameText by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf<ChatSession?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F1A))) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Chat History", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

        if (selectedSession != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedSession = null }) {
                    Text("< Back to list", fontWeight = FontWeight.Bold, color = Color(0xFF00B86B))
                }
                Text(selectedSession!!.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp), color = Color.White)
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                items(selectedSession!!.messages) { msg ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = if (msg.isFromAI) Alignment.Start else Alignment.End
                    ) {
                        Surface(
                            color = if (msg.isFromAI) Color(0xFF28283D) else Color(0xFF00B86B),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(msg.content, modifier = Modifier.padding(12.dp), fontSize = 15.sp)
                        }
                    }
                }
            }
        } else {
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No saved conversations yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sessions) { session ->
                        ListItem(
                            modifier = Modifier.clickable { selectedSession = session },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                if (editingSessionId == session.id) {
                                    OutlinedTextField(
                                        value = editNameText,
                                        onValueChange = { editNameText = it },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                if (editNameText.isNotBlank()) {
                                                    updateChatSessionName(context, session.id, editNameText)
                                                    sessions = getChatSessions(context).sortedByDescending { it.timestamp }
                                                    editingSessionId = null
                                                }
                                            }) {
                                                Icon(Icons.Default.Save, null, tint = Color(0xFF00B86B))
                                            }
                                        }
                                    )
                                } else {
                                    Text(session.name, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            supportingContent = {
                                Text("${session.platform} • ${session.messages.size} messages", color = Color.Gray)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        saveActiveChat(context, session.platform, session.messages)
                                        onRestore(session.platform)
                                    }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Restore Session", tint = Color(0xFF00B86B))
                                    }
                                    IconButton(onClick = {
                                        editingSessionId = session.id
                                        editNameText = session.name
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        deleteChatSession(context, session.id)
                                        sessions = getChatSessions(context).sortedByDescending { it.timestamp }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = Color(0xFFFF5A5F))
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthNavigationFlow(onAuthSuccess: () -> Unit) {
    var authPage by remember { mutableIntStateOf(2) }
    val activity = (LocalContext.current as? Activity)

    BackHandler {
        if (authPage != 2) {
            authPage = 2
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    when (authPage) {
        0 -> WelcomeAuthPage(
            onNavigateToLogin = { authPage = 1 },
            onNavigateToRegister = { authPage = 2 }
        )
        1 -> SignUpPage(
            onBack = { authPage = 2 },
            onSwitchToRegister = { authPage = 2 },
            onAuthSuccess = onAuthSuccess
        )
        2 -> RegisterPage(
            onBack = { activity?.moveTaskToBack(true) },
            onSwitchToLogin = { authPage = 1 },
            onAuthSuccess = onAuthSuccess
        )
    }
}

@Composable
fun WelcomeAuthPage(onNavigateToLogin: () -> Unit, onNavigateToRegister: () -> Unit) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0F1A),
            Color(0xFF1E1E2E),
            Color(0xFF11111B)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF00B86B), Color(0xFF38BDF8)))),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("M", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MultiSearch AI",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gemini • Hugging Face • Grok • Judge AI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF00B86B)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚡ Multi-Platform Intelligence", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Search once across top AI models and evaluate results side-by-side in real-time.", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B86B)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Sign In to Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = BorderStroke(1.5.dp, Color(0xFF00B86B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF00B86B))
                Spacer(Modifier.width(8.dp))
                Text("Create New Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B86B))
            }
        }
    }
}

@Composable
fun SignUpPage(onBack: () -> Unit, onSwitchToRegister: () -> Unit, onAuthSuccess: () -> Unit = {}) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF11111B))))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Sign In", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome Back!",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Sign in to access MultiSearch AI",
            fontSize = 14.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00B86B),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00B86B),
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00B86B),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00B86B),
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    coroutineScope.launch {
                        val (success, message) = performBackendLogin(context, email.trim(), password)
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            onAuthSuccess()
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B86B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Signing In...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onSwitchToRegister) {
            Text("Don't have an account? Register here", color = Color(0xFF00B86B))
        }
    }
}

@Composable
fun RegisterPage(onBack: () -> Unit, onSwitchToLogin: () -> Unit, onAuthSuccess: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF11111B))))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Create Account", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Join MultiSearch AI",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Create your account to unlock all features",
            fontSize = 14.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00B86B),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00B86B),
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00B86B),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00B86B),
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00B86B),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00B86B),
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00B86B),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00B86B),
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) {
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        coroutineScope.launch {
                            val (success, message) = performBackendRegister(context, name.trim(), email.trim(), password)
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "$message Logging in...", Toast.LENGTH_LONG).show()
                                onAuthSuccess()
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B86B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Creating Account...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            } else {
                Text("Register Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onSwitchToLogin) {
            Text("Already have an account? Sign In", color = Color(0xFF00B86B))
        }
    }
}
