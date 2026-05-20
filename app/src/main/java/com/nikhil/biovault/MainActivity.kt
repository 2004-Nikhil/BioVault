package com.nikhil.biovault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.biovault.core.data.EncryptedPrefsStore
import com.nikhil.biovault.core.data.SettingsStore
import com.nikhil.biovault.core.data.VaultRepository
import com.nikhil.biovault.core.security.AppLockState
import com.nikhil.biovault.core.security.AutoLockManager
import com.nikhil.biovault.core.security.ClipboardClearManager
import com.nikhil.biovault.core.security.ScreenCaptureBlock
import com.nikhil.biovault.core.generator.GeneratorScreen
import com.nikhil.biovault.feature.auth.AuthScreen
import com.nikhil.biovault.feature.settings.SettingsScreen
import com.nikhil.biovault.feature.settings.SettingsViewModel
import com.nikhil.biovault.feature.settings.SettingsViewModelFactory
import com.nikhil.biovault.feature.vault.AddEditScreen
import com.nikhil.biovault.feature.vault.CredentialDetailScreen
import com.nikhil.biovault.feature.vault.VaultListScreen
import com.nikhil.biovault.feature.vault.VaultViewModel
import com.nikhil.biovault.ui.theme.BioVaultTheme

sealed class Screen {
    object Auth                                                : Screen()
    object List                                                : Screen()
    object AddNew                                              : Screen()
    object Settings                                            : Screen()
    data class Detail  (val id: String)                        : Screen()
    data class Edit    (val id: String)                        : Screen()
    data class Generator(
        val returnToAddEdit: Boolean = false,
        val editId: String? = null  // non-null when launched from Edit screen
    ) : Screen()
}

class MainActivity : FragmentActivity() {

    private lateinit var autoLockManager: AutoLockManager
    private lateinit var clipboardClearManager: ClipboardClearManager
    private lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs         = EncryptedPrefsStore(this)
        settingsStore     = SettingsStore(prefs)
        autoLockManager   = AutoLockManager(settingsStore.getAutoLockTimeout())
        clipboardClearManager = ClipboardClearManager(this)
        autoLockManager.register()

        setContent {
            BioVaultTheme {
                // 1. Observe the setting from the store
                // If your store doesn't have a flow, you can collect it from the ViewModel
                // or just use a state variable updated by settingsStore
                val isCaptureBlocked by settingsStore.isScreenCaptureBlockedFlow.collectAsState()
                ScreenCaptureBlock(enabled = isCaptureBlocked)
                AppNavigation(
                    clipboardClearManager = clipboardClearManager,
                    settingsStore         = settingsStore,
                    onTimeoutChanged      = { newMs ->
                        // Rebuild AutoLockManager with new timeout on the fly
                        autoLockManager.unregister()
                        autoLockManager = AutoLockManager(newMs)
                        autoLockManager.register()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoLockManager.unregister()
        clipboardClearManager.clearNow()
    }
}

@Composable
private fun AppNavigation(
    clipboardClearManager: ClipboardClearManager,
    settingsStore: SettingsStore,
    onTimeoutChanged: (Long) -> Unit
) {
    val context = LocalContext.current
    val isLocked      by AppLockState.isLocked.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }
    val vaultViewModel: VaultViewModel = viewModel()

    // Password passback state — generator fills this, AddEdit reads it
    var pendingPassword by remember { mutableStateOf<String?>(null) }

    // Lock gate
    if (isLocked && currentScreen != Screen.Auth) {
        currentScreen = Screen.Auth
        clipboardClearManager.clearNow()
    }

    AnimatedContent(
        targetState   = currentScreen,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec  = tween(280)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(280)
            )
        },
        label = "screenTransition"
    ) { screen ->
        when (screen) {

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
                onOpenSettings     = { currentScreen = Screen.Settings },
                viewModel          = vaultViewModel
            )

            is Screen.AddNew -> {
                // Consume any password passed back from generator
                val injected = pendingPassword
                AddEditScreen(
                    injectedPassword   = injected,
                    onPasswordConsumed = { pendingPassword = null },
                    onSave = { credential ->
                        vaultViewModel.addCredential(credential)
                        currentScreen = Screen.List
                    },
                    onBack     = { currentScreen = Screen.List },
                    onGenerate = { currentScreen = Screen.Generator(returnToAddEdit = true) }
                )
            }

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
                    val injected = pendingPassword
                    AddEditScreen(
                        existingCredential = credential,
                        injectedPassword   = injected,
                        onPasswordConsumed = { pendingPassword = null },
                        onSave = { updated ->
                            vaultViewModel.updateCredential(updated)
                            currentScreen = Screen.Detail(screen.id)
                        },
                        onBack     = { currentScreen = Screen.Detail(screen.id) },
                        onGenerate = {
                            currentScreen = Screen.Generator(
                                returnToAddEdit = false,
                                editId          = screen.id
                            )
                        }
                    )
                }
            }

            is Screen.Generator -> GeneratorScreen(
                onBack = {
                    currentScreen = when {
                        screen.editId != null    -> Screen.Edit(screen.editId)
                        screen.returnToAddEdit   -> Screen.AddNew
                        else                     -> Screen.List
                    }
                },
                onUsePassword = if (screen.returnToAddEdit || screen.editId != null) {
                    { password ->
                        pendingPassword = password
                        currentScreen   = when {
                            screen.editId != null  -> Screen.Edit(screen.editId)
                            else                   -> Screen.AddNew
                        }
                    }
                } else null
            )

            is Screen.Settings -> {
                val factory = remember {
                    SettingsViewModelFactory(
                        settingsStore,
                        VaultRepository(EncryptedPrefsStore(context))
                    )
                }
                val settingsVm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = settingsVm,
                    onBack = { currentScreen = Screen.List },
                    onVaultWiped = {
                        vaultViewModel.reload()
                        currentScreen = Screen.List
                    }
                )
            }
        }
    }
}