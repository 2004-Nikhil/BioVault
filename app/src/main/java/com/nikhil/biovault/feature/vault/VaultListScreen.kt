package com.nikhil.biovault.feature.vault

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    onAddNew: () -> Unit,
    onSelectCredential: (Credential) -> Unit,
    viewModel: VaultViewModel = viewModel()
) {
    val searchQuery  = viewModel.searchQuery.value
    val filtered     = viewModel.filteredCredentials
    val darkBg       = Color(0xFF0D1117)
    val surface      = Color(0xFF161B22)

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Vault", fontWeight = FontWeight.Bold, color = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onAddNew,
                containerColor    = Color(0xFF58A6FF),
                contentColor      = Color.White
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
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = Color(0xFF8B949E))
                },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Color(0xFF58A6FF),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    cursorColor          = Color(0xFF58A6FF)
                )
            )

            // ── Credential count ────────────────────────────────────────
            Text(
                text     = "${filtered.size} item${if (filtered.size == 1) "" else "s"}",
                color    = Color(0xFF484F58),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // ── List / empty state ──────────────────────────────────────
            AnimatedVisibility(
                visible = filtered.isEmpty(),
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                EmptyState(isSearching = searchQuery.isNotEmpty())
            }

            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { credential ->
                    CredentialCard(
                        credential = credential,
                        onClick    = { onSelectCredential(credential) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialCard(credential: Credential, onClick: () -> Unit) {
    Surface(
        color    = Color(0xFF161B22),
        shape    = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier  = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle with first letter
            Box(
                modifier          = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF58A6FF).copy(alpha = 0.15f)),
                contentAlignment  = Alignment.Center
            ) {
                Text(
                    text       = credential.site.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color      = Color(0xFF58A6FF),
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
                    color    = Color(0xFF8B949E),
                    fontSize = 13.sp
                )
            }

            if (credential.totpSecret.isNotBlank()) {
                Text("2FA", fontSize = 11.sp, color = Color(0xFF3FB950),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(Color(0xFF3FB950).copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔐", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text     = if (isSearching) "No results found" else "Your vault is empty",
                color    = Color(0xFF8B949E),
                fontSize = 16.sp
            )
            Text(
                text     = if (isSearching) "Try a different search term" else "Tap + to add your first credential",
                color    = Color(0xFF484F58),
                fontSize = 13.sp
            )
        }
    }
}