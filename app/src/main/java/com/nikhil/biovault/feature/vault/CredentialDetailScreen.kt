package com.nikhil.biovault.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikhil.biovault.core.model.Credential

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialDetailScreen(
    credential: Credential,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val clipboard     = LocalClipboardManager.current
    var showPassword  by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val darkBg  = Color(0xFF0D1117)
    val surface = Color(0xFF161B22)

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = { Text(credential.site, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = Color(0xFF8B949E))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit",
                            tint = Color(0xFF58A6FF))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(darkBg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailField(label = "Site",     value = credential.site)
            DetailField(label = "Username", value = credential.username) {
                clipboard.setText(AnnotatedString(credential.username))
            }

            // Password row with reveal + copy
            DetailCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Password", fontSize = 11.sp, color = Color(0xFF8B949E))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text       = if (showPassword) credential.password else "•".repeat(credential.password.length.coerceAtMost(20)),
                            color      = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 15.sp
                        )
                    }
                    Row {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF8B949E)
                            )
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(credential.password)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                                tint = Color(0xFF58A6FF))
                        }
                    }
                }
            }

            if (credential.totpSecret.isNotBlank()) {
                DetailField(label = "TOTP Secret", value = credential.totpSecret) {
                    clipboard.setText(AnnotatedString(credential.totpSecret))
                }
            }

            if (credential.notes.isNotBlank()) {
                DetailField(label = "Notes", value = credential.notes)
            }
        }
    }

    // ── Delete confirmation dialog ──────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete credential?") },
            text  = { Text("This will permanently remove ${credential.site} from your vault.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF161B22),
            titleContentColor = Color.White,
            textContentColor  = Color(0xFF8B949E)
        )
    }
}

@Composable
private fun DetailField(label: String, value: String, onCopy: (() -> Unit)? = null) {
    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = Color(0xFF8B949E))
                Spacer(Modifier.height(2.dp))
                Text(value, color = Color.White, fontSize = 15.sp)
            }
            if (onCopy != null) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                        tint = Color(0xFF58A6FF), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Surface(
        color  = Color(0xFF161B22),
        shape  = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}