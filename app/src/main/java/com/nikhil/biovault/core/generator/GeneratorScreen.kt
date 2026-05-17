package com.nikhil.biovault.core.generator

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.biovault.core.generator.PasswordGeneratorEngine
import com.nikhil.biovault.core.generator.GeneratedPassword
import com.nikhil.biovault.ui.components.evaluateStrength

// ── Palette ────────────────────────────────────────────────────────────────
private val DarkBg    = Color(0xFF0D1117)
private val Surface   = Color(0xFF161B22)
private val Accent    = Color(0xFF58A6FF)
private val Subtle    = Color(0xFF8B949E)
private val Border    = Color(0xFF30363D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onBack: () -> Unit,
    onUsePassword: ((String) -> Unit)? = null,  // null = standalone mode
    viewModel: GeneratorViewModel = viewModel()
) {
    val clipboard   = LocalClipboardManager.current
    val config      = viewModel.config
    val result      = viewModel.result
    val error       = viewModel.errorMessage
    var copied      by remember { mutableStateOf(false) }

    // Reset copied badge after 2s
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2_000)
            copied = false
        }
    }

    // Spin animation on regenerate
    var spinTrigger by remember { mutableStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue   = spinTrigger * 360f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label         = "refresh-spin"
    )

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Password Generator", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Subtle)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(DarkBg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Generated password display ─────────────────────────────
            PasswordDisplay(
                result    = result,
                error     = error,
                copied    = copied,
                rotation  = rotation,
                onCopy    = {
                    result?.password?.let {
                        clipboard.setText(AnnotatedString(it))
                        copied = true
                    }
                },
                onRefresh = {
                    spinTrigger++
                    viewModel.regenerate()
                }
            )

            // ── Entropy display ────────────────────────────────────────
            result?.let { EntropyDisplay(it) }

            // ── Strength bar ───────────────────────────────────────────
            result?.password?.let { pwd ->
                val strength = evaluateStrength(pwd)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Strength", fontSize = 13.sp, color = Subtle)
                        Text(strength.label, fontSize = 13.sp, color = strength.color,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    StrengthSegments(strength.score)
                }
            }

            HorizontalDivider(color = Border)

            // ── Length slider ──────────────────────────────────────────
            LengthSlider(
                length    = config.length,
                onChanged = { viewModel.setLength(it) }
            )

            HorizontalDivider(color = Border)

            // ── Character set toggles ──────────────────────────────────
            Text("Character Sets", fontSize = 13.sp,
                color = Subtle, fontWeight = FontWeight.SemiBold)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ToggleRow(
                    label   = "Uppercase  A–Z",
                    checked = config.useUppercase,
                    onToggle = { viewModel.toggleUppercase() }
                )
                ToggleRow(
                    label   = "Lowercase  a–z",
                    checked = config.useLowercase,
                    onToggle = { viewModel.toggleLowercase() }
                )
                ToggleRow(
                    label   = "Digits  0–9",
                    checked = config.useDigits,
                    onToggle = { viewModel.toggleDigits() }
                )
                ToggleRow(
                    label   = "Symbols  !@#\$…",
                    checked = config.useSymbols,
                    onToggle = { viewModel.toggleSymbols() }
                )
                ToggleRow(
                    label   = "Exclude ambiguous  0Ol1I",
                    checked = config.excludeAmbiguous,
                    onToggle = { viewModel.toggleExcludeAmbiguous() }
                )
            }

            // ── Use password button (when launched from AddEdit) ────────
            if (onUsePassword != null && result != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick  = { onUsePassword(result.password) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("Use This Password", color = Color.White,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun PasswordDisplay(
    result: GeneratedPassword?,
    error: String?,
    copied: Boolean,
    rotation: Float,
    onCopy: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color  = Surface,
        shape  = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Generated Password",
                    fontSize = 11.sp,
                    color    = Subtle,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Row {
                    // Copy button
                    IconButton(onClick = onCopy, enabled = result != null) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (copied) Color(0xFF3FB950) else Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Refresh button with spin
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = Subtle,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (error != null) {
                Text(error, color = Color(0xFFE53935), fontSize = 14.sp)
            } else {
                Text(
                    text       = result?.password ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 18.sp,
                    color      = Color.White,
                    lineHeight = 26.sp
                )
            }

            // Copied badge
            if (copied) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = "✓  Copied to clipboard",
                    fontSize = 12.sp,
                    color    = Color(0xFF3FB950)
                )
            }
        }
    }
}

@Composable
private fun EntropyDisplay(result: GeneratedPassword) {
    val entropy      = result.entropyBits
    val label        = PasswordGeneratorEngine.entropyLabel(entropy)
    val entropyColor by animateColorAsState(
        targetValue = when {
            entropy < 36  -> Color(0xFFE53935)
            entropy < 60  -> Color(0xFFFFB300)
            entropy < 80  -> Color(0xFF66BB6A)
            else          -> Color(0xFF00E676)
        },
        label = "entropyColor"
    )

    Surface(
        color  = Surface,
        shape  = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Entropy", fontSize = 11.sp, color = Subtle)
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = "%.1f bits".format(entropy),
                    color      = entropyColor,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Charset size", fontSize = 11.sp, color = Subtle)
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = "${result.charsetSize} chars",
                    color      = Color.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = entropyColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(entropyColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StrengthSegments(score: Int) {
    // 5 equal segments, filled up to score
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val colors = listOf(
            Color(0xFFE53935),
            Color(0xFFFF7043),
            Color(0xFFFFB300),
            Color(0xFF66BB6A),
            Color(0xFF00E676)
        )
        repeat(5) { index ->
            val filled       = index < score
            val segmentColor by animateColorAsState(
                targetValue   = if (filled) colors[index] else Border,
                animationSpec = tween(300),
                label         = "seg$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(segmentColor)
            )
        }
    }
}

@Composable
private fun LengthSlider(length: Int, onChanged: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("Length", fontSize = 13.sp, color = Subtle)
            Surface(
                color = Accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text     = "$length",
                    color    = Accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value         = length.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange    = 8f..64f,
            steps         = 55,   // 64 - 8 - 1
            colors        = SliderDefaults.colors(
                thumbColor       = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Border
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("8", fontSize = 11.sp, color = Subtle)
            Text("64", fontSize = 11.sp, color = Subtle)
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = Accent,
                uncheckedTrackColor = Border,
                uncheckedThumbColor = Subtle
            )
        )
    }
}