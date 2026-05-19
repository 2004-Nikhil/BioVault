package com.nikhil.biovault.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val DarkBg  = Color(0xFF0D1117)
private val Surface = Color(0xFF161B22)
private val Accent  = Color(0xFF58A6FF)
private val Subtle  = Color(0xFF8B949E)
private val Success = Color(0xFF3FB950)
private val Danger  = Color(0xFFE53935)
private val Warning = Color(0xFFE3B341)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBg, Color(0xFF161B22)))),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState   = authState,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label         = "authContent"
        ) { state ->
            when (state) {

                // ── Key invalidated — biometric enrollment changed ───────
                is AuthState.KeyInvalidated -> KeyInvalidatedPanel(
                    onAcknowledge = { viewModel.acknowledgeKeyInvalidation() }
                )

                // ── Key reset done — prompt fresh enrol ─────────────────
                is AuthState.ReadyForReEnrollment -> ReEnrollmentPanel(
                    onContinue = { viewModel.proceedAfterReEnrollment() }
                )

                // ── All normal auth states ───────────────────────────────
                else -> NormalAuthPanel(
                    authState    = state,
                    viewModel    = viewModel,
                    onUnlock     = { viewModel.authenticate(activity) }
                )
            }
        }
    }
}

// ── Normal auth panel ──────────────────────────────────────────────────────

@Composable
private fun NormalAuthPanel(
    authState: AuthState,
    viewModel: AuthViewModel,
    onUnlock: () -> Unit
) {
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
        if (authState is AuthState.Failed) triggerShake = true
    }

    val iconColor by animateColorAsState(
        targetValue = when (authState) {
            is AuthState.Authenticated -> Success
            is AuthState.Failed,
            is AuthState.LockedOut     -> Danger
            else                       -> Accent
        },
        animationSpec = tween(400),
        label = "iconColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text          = "VAULT",
            fontSize      = 13.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 6.sp,
            color         = Accent
        )

        Spacer(Modifier.height(8.dp))

        // Lock icon
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
                text     = when (authState) {
                    is AuthState.Authenticated -> "✓"
                    is AuthState.LockedOut     -> "⊘"
                    else                       -> "⬡"
                },
                fontSize = 42.sp,
                color    = iconColor
            )
        }

        Spacer(Modifier.height(8.dp))

        // Status message
        Text(
            text      = statusText(authState, viewModel),
            fontSize  = 15.sp,
            color     = Subtle,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        // Remaining attempts warning bar
        if (authState is AuthState.Failed || authState is AuthState.Idle) {
            val remaining = viewModel.remainingAttempts()
            if (remaining in 1..3) {
                AttemptsWarning(remaining)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Unlock button
        val isLocked = authState is AuthState.LockedOut
        val isAuthed = authState is AuthState.Authenticated

        Button(
            onClick  = onUnlock,
            enabled  = !isLocked && !isAuthed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = Accent,
                disabledContainerColor = Color(0xFF21262D)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text       = if (isLocked) "Vault Locked" else "Unlock with Biometric",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = if (isLocked) Color(0xFF484F58) else Color.White
            )
        }

        if (!viewModel.isBiometricAvailable()) {
            Text(
                text      = "⚠ No biometric enrolled on this device",
                fontSize  = 12.sp,
                color     = Warning,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Key invalidated panel ──────────────────────────────────────────────────

@Composable
private fun KeyInvalidatedPanel(onAcknowledge: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Warning.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("⚠", fontSize = 42.sp)
        }

        Text(
            text       = "Security Key Invalidated",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            textAlign  = TextAlign.Center
        )

        Surface(
            color  = Warning.copy(alpha = 0.08f),
            shape  = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text      = "Your biometric credentials have changed.",
                    fontSize  = 14.sp,
                    color     = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "This happens when a new fingerprint or face is enrolled, " +
                            "or all biometrics are removed from this device. " +
                            "Your vault data is safe — only the security key needs to be reset.",
                    fontSize  = 13.sp,
                    color     = Subtle,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick  = onAcknowledge,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Warning)
        ) {
            Text(
                text       = "Reset Security Key",
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF0D1117)
            )
        }

        Text(
            text      = "Your stored passwords will not be affected.",
            fontSize  = 12.sp,
            color     = Subtle,
            textAlign = TextAlign.Center
        )
    }
}

// ── Re-enrollment panel ────────────────────────────────────────────────────

@Composable
private fun ReEnrollmentPanel(onContinue: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Success.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", fontSize = 42.sp, color = Success)
        }

        Text(
            text       = "Security Key Reset",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            textAlign  = TextAlign.Center
        )

        Surface(
            color    = Success.copy(alpha = 0.08f),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text      = "A fresh security key has been generated. " +
                        "You can now unlock your vault with your current biometric.",
                fontSize  = 13.sp,
                color     = Subtle,
                lineHeight = 20.sp,
                modifier  = Modifier.padding(16.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick  = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text(
                text       = "Continue to Unlock",
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }
    }
}

// ── Attempts warning ───────────────────────────────────────────────────────

@Composable
private fun AttemptsWarning(remaining: Int) {
    Surface(
        color  = Danger.copy(alpha = 0.08f),
        shape  = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text      = "⚠  $remaining attempt${if (remaining == 1) "" else "s"} remaining before lockout",
            fontSize  = 12.sp,
            color     = Danger,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(10.dp)
        )
    }
}

// ── Status text helper ─────────────────────────────────────────────────────

private fun statusText(state: AuthState, viewModel: AuthViewModel): String =
    when (state) {
        is AuthState.Idle               -> "Touch sensor to unlock"
        is AuthState.Authenticating     -> "Verifying…"
        is AuthState.Authenticated      -> "Access granted"
        is AuthState.Failed             -> "Authentication failed — try again"
        is AuthState.BiometricUnavailable -> "No biometric enrolled. Go to Settings → Security"
        is AuthState.LockedOut          -> {
            val mins = TimeUnit.MILLISECONDS.toMinutes(state.remainingMs)
            val secs = TimeUnit.MILLISECONDS.toSeconds(state.remainingMs) % 60
            "Too many attempts — locked for %02d:%02d".format(mins, secs)
        }
        is AuthState.Error              -> state.message
        // These two are handled by AnimatedContent above, never reach here
        is AuthState.KeyInvalidated,
        is AuthState.ReadyForReEnrollment -> ""
    }