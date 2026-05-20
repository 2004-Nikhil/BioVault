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
fun ScreenCaptureBlock(enabled: Boolean) {
    val view = LocalView.current

    DisposableEffect(enabled, view) {
        val context = view.context
        // Helper to find Activity
        var currentContext = context

        val activity = (view.context as? android.app.Activity) ?: findActivity(view.context)
        val window = activity?.window

        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }


        onDispose {
            // Clear the flag when composable leaves composition
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private fun findActivity(context: android.content.Context): android.app.Activity? {
    var currentContext = context
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}