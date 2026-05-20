package com.nikhil.biovault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF58A6FF),
    onPrimary        = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1F3A5F),
    secondary        = Color(0xFF3FB950),
    onSecondary      = Color(0xFF0D1117),
    tertiary         = Color(0xFFE3B341),
    background       = Color(0xFF0D1117),
    onBackground     = Color(0xFFE6EDF3),
    surface          = Color(0xFF161B22),
    onSurface        = Color(0xFFE6EDF3),
    surfaceVariant   = Color(0xFF21262D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline          = Color(0xFF30363D),
    error            = Color(0xFFE53935),
    onError          = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun BioVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}