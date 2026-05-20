package com.nikhil.biovault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.biovault.core.totp.TotpGenerator
import com.nikhil.biovault.core.totp.TotpManager
import kotlinx.coroutines.CoroutineScope

private val Accent   = Color(0xFF58A6FF)
private val Subtle   = Color(0xFF8B949E)
private val Danger   = Color(0xFFE53935)
private val Surface  = Color(0xFF161B22)

@Composable
fun TotpCard(
    secret: String,
    modifier: Modifier = Modifier
) {
    val scope      = rememberCoroutineScope()
    val manager    = remember { TotpManager() }
    val totpCode   by manager.totpCode.collectAsState()
    val clipboard  = LocalClipboardManager.current
    var copied     by remember { mutableStateOf(false) }

    // Start ticker when card enters composition, stop on exit
    DisposableEffect(secret) {
        manager.start(secret, scope)
        onDispose { manager.stop() }
    }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2_000)
            copied = false
        }
    }

    Surface(
        color    = Surface,
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Countdown ring ─────────────────────────────────────────
            totpCode?.let { TotpRing(it) }

            Spacer(Modifier.width(16.dp))

            // ── Code + label ───────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = "ONE-TIME PASSWORD",
                    fontSize  = 10.sp,
                    color     = Subtle,
                    letterSpacing = 1.sp,
                    fontWeight    = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))

                if (totpCode != null) {
                    // Split into two groups of 3 for readability: 047 382
                    val display = totpCode!!.code.chunked(3).joinToString(" ")
                    Text(
                        text       = display,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color      = codeColor(totpCode!!.secondsRemaining),
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = "${totpCode!!.secondsRemaining}s remaining",
                        fontSize = 11.sp,
                        color    = Subtle
                    )
                } else {
                    Text(
                        text     = "Invalid secret",
                        fontSize = 14.sp,
                        color    = Danger
                    )
                }
            }

            // ── Copy button ────────────────────────────────────────────
            if (totpCode != null) {
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(totpCode!!.code))
                    copied = true
                }) {
                    Icon(
                        imageVector        = Icons.Default.ContentCopy,
                        contentDescription = "Copy OTP",
                        tint               = if (copied) Color(0xFF3FB950) else Accent,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Animated countdown ring ────────────────────────────────────────────────

@Composable
private fun TotpRing(code: TotpGenerator.TotpCode) {
    val animatedProgress by animateFloatAsState(
        targetValue   = code.progress,
        animationSpec = tween(durationMillis = 800),
        label         = "totpProgress"
    )
    val ringColor = codeColor(code.secondsRemaining)

    Box(
        modifier          = Modifier
            .size(52.dp)
            .drawBehind {
                val strokeWidth = 5.dp.toPx()
                val radius      = (size.minDimension - strokeWidth) / 2f
                val topLeft     = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize     = Size(radius * 2f, radius * 2f)

                // Background track
                drawArc(
                    color       = Color(0xFF30363D),
                    startAngle  = -90f,
                    sweepAngle  = 360f,
                    useCenter   = false,
                    topLeft     = topLeft,
                    size        = arcSize,
                    style       = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Progress arc
                drawArc(
                    color       = ringColor,
                    startAngle  = -90f,
                    sweepAngle  = 360f * animatedProgress,
                    useCenter   = false,
                    topLeft     = topLeft,
                    size        = arcSize,
                    style       = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text     = "${code.secondsRemaining}",
            fontSize = 13.sp,
            color    = ringColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Ring + code turns red when < 5 seconds remain
@Composable
private fun codeColor(secondsRemaining: Int): Color {
    val color by animateColorAsState(
        targetValue   = if (secondsRemaining <= 5) Danger else Accent,
        animationSpec = tween(500),
        label         = "codeColor"
    )
    return color
}