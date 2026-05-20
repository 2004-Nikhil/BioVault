package com.nikhil.biovault.feature.settings


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.biovault.core.data.SettingsStore

private val DarkBg  = Color(0xFF0D1117)
private val Surface = Color(0xFF161B22)
private val Accent  = Color(0xFF58A6FF)
private val Subtle  = Color(0xFF8B949E)
private val Danger  = Color(0xFFE53935)
private val Border  = Color(0xFF30363D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onVaultWiped: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    var showTimeoutSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Security section ───────────────────────────────────────
            SettingsSectionHeader("Security")

            SettingsCard {
                // Auto-lock timeout
                SettingsRow(
                    label    = "Auto-lock timeout",
                    subtitle = viewModel.selectedTimeoutLabel,
                    onClick  = { showTimeoutSheet = true }
                )

                HorizontalDivider(
                    color    = Border,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Screen capture block toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Block screenshots",
                            color      = Color.White,
                            fontSize   = 15.sp
                        )
                        Text(
                            "Prevent screen capture on all screens",
                            color    = Subtle,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked         = viewModel.screenCaptureBlocked,
                        onCheckedChange = { viewModel.toggleScreenCapture() },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor   = Color.White,
                            checkedTrackColor   = Accent,
                            uncheckedTrackColor = Border
                        )
                    )
                }
            }

            // ── Vault section ──────────────────────────────────────────
            SettingsSectionHeader("Vault")

            SettingsCard {
                // Credential count info row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Stored credentials", color = Color.White, fontSize = 15.sp)
                    Text(
                        "${viewModel.credentialCount}",
                        color      = Accent,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 4.dp))

                // Wipe vault
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showWipeConfirm = true }
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Wipe vault",
                            color      = Danger,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Permanently delete all ${viewModel.credentialCount} credentials",
                            color    = Subtle,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── About section ──────────────────────────────────────────
            SettingsSectionHeader("About")

            SettingsCard {
                SettingsInfoRow(label = "Version",  value = "1.0.0")
                HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 4.dp))
                SettingsInfoRow(label = "Encryption", value = "AES-256-GCM")
                HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 4.dp))
                SettingsInfoRow(label = "Biometric",  value = "Android Keystore")
                HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 4.dp))
                SettingsInfoRow(label = "TOTP",       value = "RFC 6238 / HMAC-SHA1")
            }
        }
    }

    // ── Auto-lock timeout bottom sheet ─────────────────────────────────
    if (showTimeoutSheet) {
        TimeoutPickerSheet(
            current  = viewModel.selectedTimeoutLabel,
            onSelect = {
                viewModel.setAutoLockTimeout(it)
                showTimeoutSheet = false
            },
            onDismiss = { showTimeoutSheet = false }
        )
    }

    // ── Wipe confirmation dialog ───────────────────────────────────────
    if (viewModel.showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.showWipeConfirm = false },
            title = { Text("Wipe entire vault?") },
            text  = {
                Text(
                    "This will permanently delete all ${viewModel.credentialCount} " +
                            "credentials. This action cannot be undone.",
                    color = Subtle
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.wipeVault()
                    onVaultWiped()
                }) {
                    Text("Wipe Vault", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showWipeConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor    = Surface,
            titleContentColor = Color.White,
            textContentColor  = Subtle
        )
    }
}

// ── Bottom sheet for timeout picker ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeoutPickerSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Surface
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text     = "Auto-lock timeout",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color    = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            HorizontalDivider(color = Border)

            SettingsStore.TIMEOUT_OPTIONS.keys.forEach { label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(label) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(label, color = Color.White, fontSize = 15.sp)
                    if (label == current) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(color = Border)
            }
        }
    }
}

// ── Shared helper composables ──────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text          = title.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color         = Subtle
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color    = Surface,
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(label,    color = Color.White, fontSize = 15.sp)
            Text(subtitle, color = Subtle,      fontSize = 12.sp)
        }
        Text("›", color = Subtle, fontSize = 20.sp)
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Subtle,       fontSize = 14.sp)
        Text(value, color = Color.White,  fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}