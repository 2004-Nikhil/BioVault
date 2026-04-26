package com.nikhil.biovault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import com.nikhil.biovault.feature.auth.AuthScreen

// FragmentActivity is REQUIRED — BiometricPrompt needs it
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isAuthenticated by remember { mutableStateOf(false) }

            if (!isAuthenticated) {
                AuthScreen(
                    onAuthenticated = { isAuthenticated = true }
                )
            } else {
                // Sprint 3 will plug in here — Vault screen
                VaultPlaceholder()
            }
        }
    }
}

@Composable
private fun VaultPlaceholder() {
    androidx.compose.material3.Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text("🔓 Vault — Sprint 3 coming next")
        }
    }
}