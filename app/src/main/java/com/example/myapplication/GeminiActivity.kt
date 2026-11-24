package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// OPay-inspired playful palette
private val OPayEmerald = Color(0xFF00B86B)
private val OPayEmeraldDark = Color(0xFF008A50)
private val OPayMintSoft = Color(0xFFE6FBF1)
private val OPaySunburst = Color(0xFFFFB020)
private val OPayCoral = Color(0xFFFF5A5F)
private val OPayCanvas = Color(0xFFF5F7F9)
private val InkPrimary = Color(0xFF111827)
private val InkSecondary = Color(0xFF6B7280)

enum class PlayfulVibe(val label: String, val badge: String) {
    CHILL("Chill", "😎"),
    HYPE("Hype", "⚡"),
    BRAINIAC("Brainiac", "🧠")
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val isPending: Boolean = false,
    val reaction: String? = null
)

class GeminiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OPayCanvas
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
    var selectedVibe by remember { mutableStateOf(PlayfulVibe.HYPE) }
    var nextId by remember { mutableStateOf(2L) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = 1L,
                text = "Heyy! I'm Gemini in OPay-mode 🚀 Pick a vibe up top or tap a quick chip below to kick things off!",
                isUser = false
            )
        )
    }

    val quickPrompts = remember {
        listOf(
            "Give me a fun idea 💡",
            "Roast my coding habits 😂",
            "Explain Kotlin coroutines",
            "Quick daily boost ⚡"
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    fun sendPrompt(rawPrompt: String) {
        val cleaned = rawPrompt.trim()
        if (cleaned.isEmpty()) return

        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val userMsgId = nextId++
        val pendingMsgId = nextId++

        messages.add(ChatMessage(id = userMsgId, text = cleaned, isUser = true))
        userInput = ""

        coroutineScope.launch {
            messages.add(
                ChatMessage(
                    id = pendingMsgId,
                    text = "",
                    isUser = false,
                    isPending = true
                )
            )
            delay(1100)
            val pendingIndex = messages.indexOfFirst { it.id == pendingMsgId }
            if (pendingIndex != -1) {
                messages.removeAt(pendingIndex)
            }
            val reply = getPlayfulMockResponse(cleaned, selectedVibe)
            messages.add(
                ChatMessage(
                    id = nextId++,
                    text = reply,
                    isUser = false
                )
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OPayCanvas)
    ) {
        // OPay-inspired top hub header
        OPayGeminiTopBar(
            vibe = selectedVibe,
            messageCount = messages.count { !it.isPending },
            onCycleVibe = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val all = PlayfulVibe.entries
                selectedVibe = all[(all.indexOf(selectedVibe) + 1) % all.size]
            },
            onClearChat = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                messages.clear()
                messages.add(
                    ChatMessage(
                        id = nextId++,
                        text = "Fresh slate! ✨ What are we building or exploring next?",
                        isUser = false
                    )
                )
            }
        )

        // Chat stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp)
        ) {
            itemsIndexed(messages, key = { _, item -> item.id }) { _, message ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(220)) +
                            slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                ) {
                    PlayfulChatBubble(
                        message = message,
                        onToggleReaction = { emoji ->
                            val idx = messages.indexOfFirst { it.id == message.id }
                            if (idx != -1) {
                                val current = messages[idx]
                                messages[idx] = current.copy(
                                    reaction = if (current.reaction == emoji) null else emoji
                                )
                            }
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(message.text))
                        }
                    )
                }
            }
        }

        // Playful quick-action chips strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                BouncyPillChip(
                    label = prompt,
                    onClick = { sendPrompt(prompt) }
                )
            }
        }

        // Floating / docked pill composer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            color = Color.White,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Ask Gemini anything...",
                            color = InkSecondary,
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = OPayCanvas,
                        unfocusedContainerColor = OPayCanvas,
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        focusedBorderColor = OPayEmerald,
                        cursorColor = OPayEmerald,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                val canSend = userInput.isNotBlank()
                val sendScale by animateFloatAsState(
                    targetValue = if (canSend) 1f else 0.92f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "sendScale"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(sendScale)
                        .clip(CircleShape)
                        .background(if (canSend) OPayEmerald else Color(0xFFE5E7EB))
                        .clickable(enabled = canSend) { sendPrompt(userInput) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.White else InkSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OPayGeminiTopBar(
    vibe: PlayfulVibe,
    messageCount: Int,
    onCycleVibe: () -> Unit,
    onClearChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(OPayEmerald, OPayEmeraldDark)
                )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Gemini Turbo",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$messageCount messages • Instant AI",
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive vibe switcher pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onCycleVibe),
                    shape = RoundedCornerShape(50),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${vibe.badge} ${vibe.label}",
                            color = OPayEmeraldDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Switch vibe",
                            tint = OPayEmeraldDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Clear chat button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onClearChat),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Clear chat",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BouncyPillChip(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale"
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = OPayEmerald),
                onClick = onClick
            ),
        shape = RoundedCornerShape(50),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = InkPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PlayfulChatBubble(
    message: ChatMessage,
    onToggleReaction: (String) -> Unit,
    onCopy: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) OPayEmerald else Color.White
    val textColor = if (message.isUser) Color.White else InkPrimary
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 5.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 310.dp),
                color = bubbleColor,
                shape = bubbleShape,
                shadowElevation = if (message.isUser) 3.dp else 2.dp
            ) {
                if (message.isPending) {
                    TypingDotsIndicator(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                    )
                } else {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = if (message.isUser) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                    )
                }
            }
        }

        // Playful interactive reaction & copy row under AI replies
        if (!message.isUser && !message.isPending) {
            Row(
                modifier = Modifier.padding(top = 5.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("⚡", "💡", "😂").forEach { emoji ->
                    val selected = message.reaction == emoji
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleReaction(emoji)
                            },
                        shape = RoundedCornerShape(50),
                        color = if (selected) OPayMintSoft else Color.White.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopy()
                        },
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "Copy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OPayEmeraldDark,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingDotsIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val dotColors = listOf(OPayEmerald, OPaySunburst, OPayCoral)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotColors.forEachIndexed { index, color ->
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = -7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 360, delayMillis = index * 110),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotOffset_$index"
            )

            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

fun getPlayfulMockResponse(input: String, vibe: PlayfulVibe): String {
    val lowerInput = input.lowercase()
    val prefix = when (vibe) {
        PlayfulVibe.CHILL -> "Easy mode 😎 — "
        PlayfulVibe.HYPE -> "LET'S GOOO ⚡ — "
        PlayfulVibe.BRAINIAC -> "Big-brain breakdown 🧠 — "
    }

    val coreReply = when {
        lowerInput.contains("hello") || lowerInput.contains("hi") || lowerInput.contains("hey") ->
            "What's good! Ready to brainstorm, debug, or bounce ideas around?"
        lowerInput.contains("roast") ->
            "You have 27 open tabs, 14 TODO comments from last month, and you still run 'just one quick build' at midnight 😅"
        lowerInput.contains("coroutine") ->
            "Coroutines are lightweight threads on steroids: launch them in a scope, suspend cleanly without blocking UI, and cancel safely!"
        lowerInput.contains("idea") ->
            "Build a 1-tap 'AI Battle' card where Gemini, Grok, and Judge AI score each other's jokes in real time!"
        lowerInput.contains("boost") ->
            "Ship small, test fast, and keep momentum rolling—one clean commit today beats perfection tomorrow."
        else ->
            "Love that prompt: \"$input\". Want me to turn that into a step-by-step plan or a fun mini-challenge?"
    }

    return prefix + coreReply
}