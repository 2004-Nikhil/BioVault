package com.nikhil.biovault.feature.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.nikhil.biovault.core.model.Credential

private val darkBg  = Color(0xFF0D1117)
private val surface = Color(0xFF161B22)
private val Accent  = Color(0xFF58A6FF)
private val Subtle  = Color(0xFF8B949E)
private val Danger  = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    onAddNew: () -> Unit,
    onSelectCredential: (Credential) -> Unit,
    onOpenGenerator: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: VaultViewModel = viewModel()
) {
    val searchQuery  = viewModel.searchQuery.value
    val filtered     = viewModel.filteredCredentials

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Vault", fontWeight = FontWeight.Bold, color = Color.White)
                },
                actions = {
                    IconButton(onClick = onOpenGenerator) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, null, tint = Subtle)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onAddNew,
                containerColor = Accent,
                contentColor   = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add credential")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(darkBg)
        ) {
            // ── Search bar ──────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder   = { Text("Search sites, usernames…", color = Color(0xFF484F58)) },
                leadingIcon   = {
                    Icon(Icons.Default.Search, null, tint = Subtle)
                },
                singleLine = true,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Accent,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = Accent
                )
            )

            Text(
                text     = "${filtered.size} item${if (filtered.size == 1) "" else "s"}",
                color    = Color(0xFF484F58),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // ── Empty state ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = filtered.isEmpty(),
                enter   = fadeIn(tween(300)),
                exit    = fadeOut(tween(200))
            ) {
                EmptyState(isSearching = searchQuery.isNotEmpty())
            }

            // ── Credential list with swipe-to-delete ───────────────────
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { credential ->
                    SwipeToDeleteCard(
                        credential = credential,
                        onDelete   = { viewModel.deleteCredential(credential.id) },
                        onClick    = { onSelectCredential(credential) }
                    )
                }
            }
        }
    }
}

// ── Swipe-to-delete wrapper ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteCard(
    credential: Credential,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showConfirm = true
            }
            // Return false — don't actually dismiss until confirmed
            false
        }
    )

    SwipeToDismissBox(
        state            = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Red delete background revealed during swipe
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Danger.copy(alpha = 0.15f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Delete", color = Danger, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.Delete, null, tint = Danger)
                }
            }
        }
    ) {
        CredentialCard(credential = credential, onClick = onClick)
    }

    // Confirm dialog
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete credential?") },
            text  = {
                Text(
                    "Permanently remove ${credential.site} from your vault.",
                    color = Subtle
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            },
            containerColor    = surface,
            titleContentColor = Color.White,
            textContentColor  = Subtle
        )
    }
}

// ── Credential card ────────────────────────────────────────────────────────

@Composable
private fun CredentialCard(credential: Credential, onClick: () -> Unit) {
    Surface(
        color    = surface,
        shape    = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier  = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = credential.site.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color      = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = credential.site,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = credential.username,
                    color    = Subtle,
                    fontSize = 13.sp
                )
            }

            if (credential.totpSecret.isNotBlank()) {
                Text(
                    text     = "2FA",
                    fontSize = 11.sp,
                    color    = Color(0xFF3FB950),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(Color(0xFF3FB950).copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔐", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text     = if (isSearching) "No results found" else "Your vault is empty",
                color    = Subtle,
                fontSize = 16.sp
            )
            Text(
                text     = if (isSearching) "Try a different search term"
                else "Tap + to add your first credential",
                color    = Color(0xFF484F58),
                fontSize = 13.sp
            )
        }
    }
}