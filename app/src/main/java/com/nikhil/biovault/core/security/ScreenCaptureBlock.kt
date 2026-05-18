package com.nikhil.biovault.core.security

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Drop this composable anywhere in the tree to block screenshots
 * and screen recording for every screen it's present on.
 *
 * Uses DisposableEffect so the flag is automatically cleared
 * if this composable leaves composition.
 */
@Composable
fun ScreenCaptureBlock() {
    val view = LocalView.current

    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window

        // Set FLAG_SECURE — blocks screenshots + screen recording
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        onDispose {
            // Clear the flag when composable leaves composition
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}