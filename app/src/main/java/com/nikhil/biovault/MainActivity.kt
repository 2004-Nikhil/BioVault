package com.nikhil.biovault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.biovault.core.security.AppLockState
import com.nikhil.biovault.core.security.AutoLockManager
import com.nikhil.biovault.core.security.ClipboardClearManager
import com.nikhil.biovault.core.security.ScreenCaptureBlock
import com.nikhil.biovault.core.generator.GeneratorScreen
import com.nikhil.biovault.feature.auth.AuthScreen
import com.nikhil.biovault.feature.vault.AddEditScreen
import com.nikhil.biovault.feature.vault.CredentialDetailScreen
import com.nikhil.biovault.feature.vault.VaultListScreen
import com.nikhil.biovault.feature.vault.VaultViewModel

sealed class Screen {
    object Auth                                            : Screen()
    object List                                            : Screen()
    object AddNew                                          : Screen()
    data class Detail(val id: String)                      : Screen()
    data class Edit(val id: String)                        : Screen()
    data class Generator(val returnToAddEdit: Boolean = false) : Screen()
}

class MainActivity : FragmentActivity() {

    private lateinit var autoLockManager: AutoLockManager
    private lateinit var clipboardClearManager: ClipboardClearManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        autoLockManager       = AutoLockManager(gracePeriodMs = 15_000L)
        clipboardClearManager = ClipboardClearManager(this)
        autoLockManager.register()

        setContent {
            ScreenCaptureBlock()
            AppNavigation(clipboardClearManager = clipboardClearManager)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoLockManager.unregister()
        clipboardClearManager.clearNow()
    }
}

@Composable
private fun AppNavigation(clipboardClearManager: ClipboardClearManager) {

    val isLocked by AppLockState.isLocked.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }
    val vaultViewModel: VaultViewModel = viewModel()

    // React to Lock State Changes
    LaunchedEffect(isLocked) {
        if (isLocked) {
            currentScreen = Screen.Auth
            clipboardClearManager.clearNow()
        }
    }

    when (val screen = currentScreen) {
        is Screen.Auth -> AuthScreen(
            onAuthenticated = {
                AppLockState.unlock()
                currentScreen = Screen.List
            }
        )

        is Screen.List -> VaultListScreen(
            onAddNew           = { currentScreen = Screen.AddNew },
            onSelectCredential = { currentScreen = Screen.Detail(it.id) },
            onOpenGenerator    = { currentScreen = Screen.Generator() },
            viewModel          = vaultViewModel
        )

        is Screen.AddNew -> AddEditScreen(
            onSave = { credential ->
                vaultViewModel.addCredential(credential)
                currentScreen = Screen.List
            },
            onBack     = { currentScreen = Screen.List },
            onGenerate = { currentScreen = Screen.Generator(returnToAddEdit = true) }
        )

        is Screen.Detail -> {
            val credential = vaultViewModel.getById(screen.id)
            if (credential != null) {
                CredentialDetailScreen(
                    credential            = credential,
                    onEdit                = { currentScreen = Screen.Edit(screen.id) },
                    onDelete              = {
                        vaultViewModel.deleteCredential(screen.id)
                        currentScreen = Screen.List
                    },
                    onBack                = { currentScreen = Screen.List },
                    clipboardClearManager = clipboardClearManager
                )
            }
        }

        is Screen.Edit -> {
            val credential = vaultViewModel.getById(screen.id)
            if (credential != null) {
                AddEditScreen(
                    existingCredential = credential,
                    onSave = { updated ->
                        vaultViewModel.updateCredential(updated)
                        currentScreen = Screen.Detail(screen.id)
                    },
                    onBack     = { currentScreen = Screen.Detail(screen.id) },
                    onGenerate = { currentScreen = Screen.Generator(returnToAddEdit = true) }
                )
            }
        }

        is Screen.Generator -> GeneratorScreen(
            onBack = {
                currentScreen = if (screen.returnToAddEdit) Screen.AddNew else Screen.List
            },
            onUsePassword = if (screen.returnToAddEdit) {
                { _ -> currentScreen = Screen.AddNew }
            } else null
        )
    }
}