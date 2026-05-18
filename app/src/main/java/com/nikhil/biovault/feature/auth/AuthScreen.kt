package com.nikhil.biovault.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context   = LocalContext.current
    val activity  = context as FragmentActivity
    val authState by viewModel.authState.collectAsState()

    // Trigger check on first composition
    LaunchedEffect(Unit) {
        viewModel.checkInitialState()

        if (viewModel.isBiometricAvailable() && !viewModel.authState.value.let { it is AuthState.LockedOut }) {
            viewModel.authenticate(activity)
        }
    }

    // Navigate when authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            delay(300) // brief pause so user sees success state
            onAuthenticated()
        }
    }

    // Pulse animation for the lock icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Shake animation on failed attempt
    var triggerShake by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (triggerShake) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 400
            0f  at 0
            -18f at 50
            18f  at 100
            -14f at 150
            14f  at 200
            -8f  at 250
            8f   at 300
            0f   at 400
        },
        finishedListener = { triggerShake = false },
        label = "shake"
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Failed) {
            triggerShake = true
        }
    }

    val iconColor by animateColorAsState(
        targetValue = when (authState) {
            is AuthState.Authenticated -> Color(0xFF4CAF50)
            is AuthState.Failed,
            is AuthState.LockedOut     -> Color(0xFFE53935)
            else                       -> Color(0xFF90CAF9)
        },
        animationSpec = tween(400),
        label = "iconColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1117), Color(0xFF161B22))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {

            // ── App title ──────────────────────────────────────────────
            Text(
                text = "VAULT",
                fontSize  = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = Color(0xFF58A6FF)
            )

            Spacer(Modifier.height(8.dp))

            // ── Lock icon with pulse + shake ────────────────────────────
            Box(
                modifier = Modifier
                    .offset(x = shakeOffset.dp)
                    .scale(if (authState is AuthState.Authenticating) pulseScale else 1f)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (authState) {
                        is AuthState.Authenticated -> "✓"
                        is AuthState.LockedOut     -> "⊘"
                        else                       -> "⬡"
                    },
                    fontSize = 42.sp,
                    color    = iconColor
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Status text ─────────────────────────────────────────────
            val statusText = when (val state = authState) {
                is AuthState.Idle           -> "Touch sensor to unlock"
                is AuthState.Authenticating -> "Verifying…"
                is AuthState.Authenticated  -> "Access granted"
                is AuthState.Failed         -> {
                    val remaining = viewModel.remainingAttempts()
                    if (remaining > 0) "Try again — $remaining attempt${if (remaining == 1) "" else "s"} left"
                    else "Authentication failed"
                }
                is AuthState.LockedOut -> {
                    val mins = TimeUnit.MILLISECONDS.toMinutes(state.remainingMs)
                    val secs = TimeUnit.MILLISECONDS.toSeconds(state.remainingMs) % 60
                    "Locked — try again in %02d:%02d".format(mins, secs)
                }
                is AuthState.Error -> state.message
            }

            Text(
                text      = statusText,
                fontSize  = 15.sp,
                color     = Color(0xFF8B949E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(16.dp))

            // ── Action button ───────────────────────────────────────────
            val isLocked = authState is AuthState.LockedOut
            val isAuthed = authState is AuthState.Authenticated

            Button(
                onClick  = { viewModel.authenticate(activity) },
                enabled  = !isLocked && !isAuthed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF58A6FF),
                    disabledContainerColor = Color(0xFF21262D)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isLocked) "Vault Locked" else "Unlock with Biometric",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = if (isLocked) Color(0xFF484F58) else Color.White
                )
            }

            // ── Biometric unavailable warning ──────────────────────────
            if (!viewModel.isBiometricAvailable()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "⚠ No biometric hardware found or none enrolled",
                    fontSize  = 12.sp,
                    color     = Color(0xFFE3B341),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}