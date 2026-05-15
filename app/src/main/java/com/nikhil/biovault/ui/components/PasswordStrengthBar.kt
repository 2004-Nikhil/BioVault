package com.nikhil.biovault.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PasswordStrength(val label: String, val score: Int, val color: Color) {
    VERY_WEAK ("Very Weak",  1, Color(0xFFE53935)),
    WEAK      ("Weak",       2, Color(0xFFFF7043)),
    FAIR      ("Fair",       3, Color(0xFFFFB300)),
    STRONG    ("Strong",     4, Color(0xFF66BB6A)),
    VERY_STRONG("Very Strong",5, Color(0xFF00E676))
}

fun evaluateStrength(password: String): PasswordStrength {
    if (password.length < 4)  return PasswordStrength.VERY_WEAK
    var score = 0
    if (password.length >= 8)  score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() })     score++
    if (password.any { !it.isLetterOrDigit() })                                  score++
    return when (score) {
        0, 1 -> PasswordStrength.VERY_WEAK
        2    -> PasswordStrength.WEAK
        3    -> PasswordStrength.FAIR
        4    -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

@Composable
fun PasswordStrengthBar(password: String, modifier: Modifier = Modifier) {
    if (password.isEmpty()) return
    val strength = evaluateStrength(password)
    val fraction = strength.score / 5f

    val animatedFraction by animateFloatAsState(
        targetValue   = fraction,
        animationSpec = tween(500),
        label         = "strengthBar"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF21262D))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(strength.color)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text      = strength.label,
            fontSize  = 11.sp,
            color     = strength.color
        )
    }
}