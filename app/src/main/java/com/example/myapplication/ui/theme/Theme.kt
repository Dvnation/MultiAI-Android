package com.example.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val OPayDarkColorScheme = darkColorScheme(
    primary = OPayEmerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = OPayMintSoft,
    secondary = OPayLimePop,
    onSecondary = OPayInkDark,
    tertiary = OPayAmberPop,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC)
)

private val OPayLightColorScheme = lightColorScheme(
    primary = OPayEmerald,
    onPrimary = Color.White,
    primaryContainer = OPayMintSoft,
    onPrimaryContainer = OPayEmeraldDark,
    secondary = OPayEmeraldDark,
    onSecondary = Color.White,
    tertiary = OPayAmberPop,
    background = OPayCanvas,
    onBackground = OPayInkDark,
    surface = OPayCardWhite,
    onSurface = OPayInkDark,
    surfaceVariant = OPayMintSoft,
    onSurfaceVariant = OPayInkMuted,
    outline = OPayBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default false so the playful OPay emerald/mint identity stays consistent
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> OPayDarkColorScheme
        else -> OPayLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}