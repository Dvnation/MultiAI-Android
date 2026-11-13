package com.example.myapplication

import android.os.Bundle
import io.ktor.client.plugins.HttpTimeout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Logout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import java.util.Locale

import io.ktor.client.*
import io.ktor.client.engine.okhttp.* // Change to your engine if not using OkHttp (e.g., CIO)
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
//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
//AQ.Ab8RN6LzRbQyLRJPHXeohfeqf_bymHZZtJjA9vqwtWYG-2cc6Q
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
                    color = Color.LightGray
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
                    } else if (showOnboarding) {
                        OnboardingTutorialScreen(
                            onFinished = {
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
                        if (!isAuthenticated) {
                            AuthNavigationFlow(
                                onAuthSuccess = {
                                    isAuthenticated = true
                                }
                            )
                        } else {
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
                loadRewarded() // Reload after showing
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

class Practice2 : MultiAI()

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
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8)))),
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
                color = Color(0xFFC4B5FD)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF8B5CF6),
                strokeWidth = 2.5.dp
            )
        }
    }
}

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
                                color = Color(0xFF8B5CF6)
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
                                    color = Color(0xFFC4B5FD)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                ListItem(
                    headlineContent = { Text("Settings", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Settings, null, tint = Color(0xFF8B5CF6)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onNavigate(4) }
                )
                ListItem(
                    headlineContent = { Text("Ads Support", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.VolunteerActivism, null, tint = Color(0xFF8B5CF6)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onNavigate(5) }
                )
                ListItem(
                    headlineContent = { Text("Chat History", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.History, null, tint = Color(0xFF8B5CF6)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onNavigate(6) }
                )
                ListItem(
                    headlineContent = { Text("Backup All AI Chats ☁️", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.CloudUpload, null, tint = Color(0xFF8B5CF6)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        coroutineScope.launch { drawerState.close() }
                        backupAllChatsAtOnce(context)
                    }
                )
                ListItem(
                    headlineContent = { Text("Onboarding Tutorial", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF8B5CF6)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        coroutineScope.launch { drawerState.close() }
                        onViewOnboarding()
                    }
                )
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                ListItem(
                    headlineContent = { Text("Sign Out", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.Default.Logout, null, tint = Color(0xFFFF6B6B)) },
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
                                "ChatGPT" -> 0
                                "Gemini" -> 1
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
                    .background(Color.White)
                    .imePadding()
            ) {
                // Home Screen Header: Left = App Name, Right = MultiSearch Toggle Switch & Backup Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT: Menu Icon & App Name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF1E1E2E))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "multiAI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1E2E)
                        )
                    }

                    // RIGHT: MultiSearch Toggle Switch & Backup Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isGlobalSearch) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isGlobalSearch) Color(0xFF10B981) else Color(0xFFCBD5E1)),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MultiSearch ⚡",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGlobalSearch) Color(0xFF059669) else Color(0xFF64748B)
                                )
                                Spacer(Modifier.width(4.dp))
                                Switch(
                                    checked = isGlobalSearch,
                                    onCheckedChange = { isGlobalSearch = it },
                                    modifier = Modifier.height(24.dp),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        }

                        IconButton(onClick = { backupAllChatsAtOnce(context) }) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Backup All Chats",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // AI Model Platform Selector Pills Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center
                ) {
                    StylishNavButton(
                        "GEMINI",
                        Color(0xFF4285F4),
                        isSelected = pagerState.currentPage == 0
                    ) { onNavigate(0) }
                    StylishNavButton(
                        "HUGGINGFACE",
                        Color(0xFF05040E),
                        isSelected = pagerState.currentPage == 1
                    ) { onNavigate(1) }
                    StylishNavButton(
                        "GROK",
                        Color(0xFF6366F1),
                        isSelected = pagerState.currentPage == 2
                    ) { onNavigate(2) }
                    StylishNavButton(
                        "JUDGE AI",
                        Color(0xFF8B5CF6),
                        isSelected = pagerState.currentPage == 3
                    ) { onNavigate(3) }
                }

                val context = LocalContext.current

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
                        3 -> JudgeAiNav(navController, currentSearchWord, onSearchChange, isGlobalSearch, onOpenSettings = { fullScreenPage = 4 }, onGlobalSubmit = onGlobalSubmit, externalMessages = judgeMessages)
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Support with Ads", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { (context as? MultiAI)?.showInterstitial() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Watch Interstitial Ad")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { 
                val activity = context as? MultiAI
                activity?.showRewarded() 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Watch Rewarded Ad")
        }
        Spacer(Modifier.height(24.dp))
        Text("Support us by viewing these ads", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        BannerAdView()
    }
}

@Composable
fun SettingsPage(isGlobalSearch: Boolean, onGlobalSearchChange: (Boolean) -> Unit, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E1E2E)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Settings ⚙️",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E1E2E)
                )
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
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
                                color = Color(0xFF0F172A)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Synchronize query typing & search across Gemini, Hugging Face, and Grok simultaneously.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 18.sp
                            )
                        }

                        Switch(
                            checked = isGlobalSearch,
                            onCheckedChange = onGlobalSearchChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylishNavButton(text: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "navButtonScale"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) color else Color.White,
        contentColor = if (isSelected) Color.White else color,
        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, color),
        shadowElevation = if (isSelected) 6.dp else 1.dp,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ChatHistoryPage(onBack: () -> Unit, onRestore: (String) -> Unit = {}) {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(getChatSessions(context).sortedByDescending { it.timestamp }) }
    var editingSessionId by remember { mutableStateOf<String?>(null) }
    var editNameText by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf<ChatSession?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Chat History", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider()

        if (selectedSession != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedSession = null }) {
                    Text("< Back to list", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Text(selectedSession!!.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                items(selectedSession!!.messages) { msg ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalAlignment = if (msg.isFromAI) Alignment.Start else Alignment.End
                    ) {
                        Surface(
                            color = if (msg.isFromAI) Color(0xFF343541) else Color(0xFF131312),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(msg.content, modifier = Modifier.padding(12.dp), fontSize = 16.sp)
                        }
                    }
                }
            }
        } else {
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No saved conversations yet.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(sessions) { session ->
                        ListItem(
                            modifier = Modifier.clickable { selectedSession = session },
                            headlineContent = {
                                if (editingSessionId == session.id) {
                                    OutlinedTextField(
                                        value = editNameText,
                                        onValueChange = { editNameText = it },
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                if (editNameText.isNotBlank()) {
                                                    updateChatSessionName(context, session.id, editNameText)
                                                    sessions = getChatSessions(context).sortedByDescending { it.timestamp }
                                                    editingSessionId = null
                                                }
                                            }) {
                                                Icon(Icons.Default.Save, null)
                                            }
                                        }
                                    )
                                } else {
                                    Text(session.name)
                                }
                            },
                            supportingContent = { 
                                Text("${session.platform} • ${session.messages.size} messages") 
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        saveActiveChat(context, session.platform, session.messages)
                                        onRestore(session.platform)
                                    }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Restore Session", tint = Color.Blue)
                                    }
                                    IconButton(onClick = {
                                        editingSessionId = session.id
                                        editNameText = session.name
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                                    }
                                    IconButton(onClick = {
                                        deleteChatSession(context, session.id)
                                        sessions = getChatSessions(context).sortedByDescending { it.timestamp }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = Color.Red)
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

//suspend fun mockChatGPTResponse(query: String): String {
//    delay(1000) // Simulate network delay
//    return "This is a mock ChatGPT response to: $query"
//}
//
// "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"


// Put your new, active restricted key directly here as a string

//header(HttpHeaders.Authorization, "Bearer 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")

//val geminiApiKey = "AQ.Ab8RN6LBo4isIYAY4Pg8hzR2ZZ1KRr8rCssYLxR1tzMX3JqyqA"



private val client = HttpClient(OkHttp) {

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 30_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 120_000
    }
}

suspend fun performBackendLogin(context: android.content.Context, email: String, pass: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        val maxAttempts = 2
        var lastError = "Unable to connect to backend server."

        for (attempt in 1..maxAttempts) {
            try {
                val jsonPayload = JsonObject(
                    mapOf(
                        "email" to JsonPrimitive(email),
                        "password" to JsonPrimitive(pass)
                    )
                )

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonPayload.toString().toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url("https://multisearch-ai-backend.onrender.com/auth/login")
                    .post(requestBody)
                    .build()

                val response = sharedOkHttpClient.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                if (responseText.isBlank()) {
                    lastError = "Empty server response received."
                    continue
                }

                val jsonElement = Json.parseToJsonElement(responseText).jsonObject
                val isSuccess = jsonElement["success"]?.jsonPrimitive?.boolean ?: false

                if (isSuccess && response.isSuccessful) {
                    val token = jsonElement["accessToken"]?.jsonPrimitive?.content ?: ""
                    val refreshToken = jsonElement["refreshToken"]?.jsonPrimitive?.content ?: ""
                    val message = jsonElement["message"]?.jsonPrimitive?.content ?: "Logged in successfully!"
                    if (token.isNotBlank()) {
                        saveAuthCredentials(context, token, refreshToken, email)
                    }
                    return@withContext Pair(true, message)
                } else {
                    val errorMsg = jsonElement["error"]?.jsonPrimitive?.content ?: "Login failed (${response.code})"
                    return@withContext Pair(false, errorMsg)
                }
            } catch (e: Exception) {
                lastError = if (e.localizedMessage?.contains("SSL", ignoreCase = true) == true || e.localizedMessage?.contains("timeout", ignoreCase = true) == true) {
                    "Server is waking up. Retrying connection..."
                } else {
                    "Connection error: ${e.localizedMessage ?: "Unable to connect"}"
                }
                if (attempt < maxAttempts) {
                    kotlinx.coroutines.delay(2000L)
                }
            }
        }
        Pair(false, lastError.replace("Retrying connection...", "Please tap Sign In again."))
    }
}

suspend fun performBackendRegister(context: android.content.Context, name: String, email: String, pass: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        val maxAttempts = 2
        var lastError = "Unable to connect to backend server."

        for (attempt in 1..maxAttempts) {
            try {
                val jsonPayload = JsonObject(
                    mapOf(
                        "name" to JsonPrimitive(name),
                        "email" to JsonPrimitive(email),
                        "password" to JsonPrimitive(pass)
                    )
                )

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = jsonPayload.toString().toRequestBody(mediaType)

                val request = okhttp3.Request.Builder()
                    .url("https://multisearch-ai-backend.onrender.com/auth/register")
                    .post(requestBody)
                    .build()

                val response = sharedOkHttpClient.newCall(request).execute()
                val responseText = response.body?.string() ?: ""

                if (responseText.isBlank()) {
                    lastError = "Empty server response received."
                    continue
                }

                val jsonElement = Json.parseToJsonElement(responseText).jsonObject
                val isSuccess = jsonElement["success"]?.jsonPrimitive?.boolean ?: false

                if (isSuccess && response.isSuccessful) {
                    val token = jsonElement["accessToken"]?.jsonPrimitive?.content ?: ""
                    val refreshToken = jsonElement["refreshToken"]?.jsonPrimitive?.content ?: ""
                    val message = jsonElement["message"]?.jsonPrimitive?.content ?: "Account created successfully!"
                    if (token.isNotBlank()) {
                        saveAuthCredentials(context, token, refreshToken, email)
                    }
                    return@withContext Pair(true, message)
                } else {
                    val errorMsg = jsonElement["error"]?.jsonPrimitive?.content ?: "Registration failed (${response.code})"
                    return@withContext Pair(false, errorMsg)
                }
            } catch (e: Exception) {
                lastError = if (e.localizedMessage?.contains("SSL", ignoreCase = true) == true || e.localizedMessage?.contains("timeout", ignoreCase = true) == true) {
                    "Server is waking up. Retrying connection..."
                } else {
                    "Connection error: ${e.localizedMessage ?: "Unable to connect"}"
                }
                if (attempt < maxAttempts) {
                    kotlinx.coroutines.delay(2000L)
                }
            }
        }
        Pair(false, lastError.replace("Retrying connection...", "Please tap Register again."))
    }
}

@Composable
fun AuthNavigationFlow(onAuthSuccess: () -> Unit) {
    var authPage by remember { mutableIntStateOf(2) } // Starts directly on Register / Sign Up page for new installs
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
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8)))),
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
                color = Color(0xFFC4B5FD)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Sign In to Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFFC4B5FD))
                Spacer(Modifier.width(8.dp))
                Text("Create New Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC4B5FD))
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF8B5CF6),
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
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF8B5CF6),
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
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
            Text("Don't have an account? Register here", color = Color(0xFFC4B5FD))
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF8B5CF6),
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
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF8B5CF6),
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
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF8B5CF6),
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
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF8B5CF6),
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
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
            Text("Already have an account? Sign In", color = Color(0xFFC4B5FD))
        }
    }
}
