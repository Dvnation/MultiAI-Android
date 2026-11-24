package com.example.myapplication

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val TutorialEmerald = Color(0xFF00B86B)
private val TutorialEmeraldDark = Color(0xFF008A50)
private val TutorialMintSoft = Color(0xFFE6FBF1)
private val TutorialCanvas = Color(0xFFF5F7F9)
private val TutorialInk = Color(0xFF111827)
private val TutorialMuted = Color(0xFF6B7280)

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
private fun Modifier.tutorialBounce(onClick: () -> Unit): Modifier {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tutorialBounce"
    )
    return this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TutorialCanvas
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TutorialCanvas)
                .padding(20.dp)
        ) {
            // Top bar with Back & Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TutorialInk
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (pagerState.currentPage < 4) {
                    TextButton(onClick = completeTutorial) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White
                        ) {
                            Text(
                                text = "Skip",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TutorialEmeraldDark,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 5-Step Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = 104.dp)
            ) { page ->
                when (page) {
                    0 -> OnboardingStepCard(
                        emoji = "👋",
                        badgeText = "STEP 1 OF 5",
                        title = "Welcome to multiAI",
                        description = "Your playful all-in-one AI hub. Hop across top AI assistants, compare answers side-by-side, and move fast."
                    )
                    1 -> OnboardingStepCard(
                        emoji = "🤖",
                        badgeText = "STEP 2 OF 5",
                        title = "Meet Your AI Squad",
                        description = "Switch effortlessly between Gemini, Hugging Face, Grok, and Judge AI—each bringing its own speed and style."
                    )
                    2 -> OnboardingStepCard(
                        emoji = "⚡",
                        badgeText = "STEP 3 OF 5",
                        title = "MultiSearch Mode",
                        description = "Flip on MultiSearch to type once and fire prompts across Gemini, Hugging Face, and Grok at the same time!"
                    )
                    3 -> OnboardingStepCard(
                        emoji = "⚖️",
                        badgeText = "STEP 4 OF 5",
                        title = "Compare, Contrast & Merge",
                        description = "Put answers head-to-head and let Judge AI compare, contrast, merge, or tabulate responses in seconds."
                    )
                    4 -> OnboardingStepCard(
                        emoji = "💬",
                        badgeText = "STEP 5 OF 5",
                        title = "History & Cloud Backup",
                        description = "Save standout replies, back up full chats in one tap, and keep your conversations synced."
                    )
                }
            }

            // Bottom indicator dots + CTA button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 18.dp)
                ) {
                    repeat(5) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 10.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) TutorialEmerald else TutorialEmerald.copy(alpha = 0.22f)
                                )
                        )
                    }
                }

                if (pagerState.currentPage == 4) {
                    Button(
                        onClick = completeTutorial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .tutorialBounce {},
                        colors = ButtonDefaults.buttonColors(containerColor = TutorialEmerald),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Let's Get Started 🚀",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                            .height(54.dp)
                            .tutorialBounce {},
                        colors = ButtonDefaults.buttonColors(containerColor = TutorialEmerald),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Text(
                            text = "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
    val infiniteTransition = rememberInfiniteTransition(label = "emojiFloat")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(116.dp)
                    .offset(y = offsetY.dp),
                shape = RoundedCornerShape(32.dp),
                color = TutorialMintSoft,
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(listOf(TutorialEmerald, TutorialEmeraldDark))
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 54.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = TutorialMintSoft,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TutorialEmeraldDark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TutorialInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = TutorialMuted,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
        }
    }
}