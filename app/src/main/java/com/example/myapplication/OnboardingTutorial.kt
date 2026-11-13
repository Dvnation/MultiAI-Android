package com.example.myapplication

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

fun hasCompletedOnboarding(context: Context): Boolean {
    val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("has_completed_onboarding", false)
}

fun setOnboardingCompleted(context: Context) {
    val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("has_completed_onboarding", true).apply()
}

fun resetOnboardingState(context: Context) {
    val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("has_completed_onboarding", false).apply()
}

@Composable
fun OnboardingTutorialScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 5 })

    val activity = (context as? Activity)
    BackHandler {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    val completeTutorial = {
        setOnboardingCompleted(context)
        onFinished()
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F0F1A),
            Color(0xFF1E1E2E),
            Color(0xFF11111B)
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0F1A)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(20.dp)
        ) {
            // TOP HEADER: SKIP BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (pagerState.currentPage < 4) {
                    TextButton(onClick = completeTutorial) {
                        Text(
                            text = "Skip",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC4B5FD)
                        )
                    }
                }
            }

            // PAGER FOR 5 ONBOARDING SCREENS
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp, bottom = 100.dp)
            ) { page ->
                when (page) {
                    0 -> OnboardingStepCard(
                        emoji = "👋",
                        badgeText = "STEP 1 OF 5",
                        title = "Welcome to multiAI",
                        description = "Your all-in-one AI productivity platform. Interact with multiple top-tier AI assistants, compare answers side-by-side, and boost your daily workflow."
                    )
                    1 -> OnboardingStepCard(
                        emoji = "🤖",
                        badgeText = "STEP 2 OF 5",
                        title = "Meet Your AI Assistants",
                        description = "Easily switch between Gemini, Hugging Face, Grok, and Cloudflare Judge AI. Each assistant brings unique strengths and specialized insights to your questions."
                    )
                    2 -> OnboardingStepCard(
                        emoji = "⚡",
                        badgeText = "STEP 3 OF 5",
                        title = "Same Search Mode",
                        description = "Enable 'Same Search' in Settings to type once and query Gemini, Hugging Face, and Grok simultaneously! Judge AI remains separate for unbiased evaluations."
                    )
                    3 -> OnboardingStepCard(
                        emoji = "🧠",
                        badgeText = "STEP 4 OF 5",
                        title = "Compare & Judge Answers",
                        description = "View responses live side-by-side. Use Judge AI to Compare, Contrast, Merge, or Tabulate answers from all AI models into clean Markdown tables."
                    )
                    4 -> OnboardingStepCard(
                        emoji = "💬",
                        badgeText = "STEP 5 OF 5",
                        title = "Chat History & Cloud Sync",
                        description = "Save individual prompts, backup full conversations, and access your account's synchronized chat history seamlessly across all your devices."
                    )
                }
            }

            // BOTTOM NAVIGATION ROW
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ANIMATED STEP INDICATORS (DOTS)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    repeat(5) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 10.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.25f))
                        )
                    }
                }

                // ACTION BUTTONS
                if (pagerState.currentPage == 4) {
                    Button(
                        onClick = completeTutorial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Let's Get Started 🚀",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingStepCard(
    emoji: String,
    badgeText: String,
    title: String,
    description: String
) {
    // EMOJI FLOATING / BOUNCING ANIMATION
    val infiniteTransition = rememberInfiniteTransition(label = "emojiFloat")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // EMOJI ICON CONTAINER
        Surface(
            modifier = Modifier
                .size(130.dp)
                .offset(y = offsetY.dp),
            shape = RoundedCornerShape(36.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8)))),
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = emoji,
                    fontSize = 60.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // STEP BADGE
        Surface(
            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF8B5CF6))
        ) {
            Text(
                text = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFC4B5FD),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TITLE
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // DESCRIPTION
        Text(
            text = description,
            fontSize = 15.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
