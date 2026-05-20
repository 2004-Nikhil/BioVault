package com.nikhil.biovault.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nikhil.biovault.core.model.Credential
import com.nikhil.biovault.ui.components.PasswordStrengthBar
import com.nikhil.biovault.core.totp.Base32

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    existingCredential: Credential? = null,
    onSave: (Credential) -> Unit,
    onBack: () -> Unit,
    onGenerate: (() -> Unit)? = null
) {
    var site        by remember { mutableStateOf(existingCredential?.site        ?: "") }
    var username    by remember { mutableStateOf(existingCredential?.username    ?: "") }
    var password    by remember { mutableStateOf(existingCredential?.password    ?: "") }
    var totpSecret  by remember { mutableStateOf(existingCredential?.totpSecret  ?: "") }
    var notes       by remember { mutableStateOf(existingCredential?.notes       ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    var siteError     by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var totpError by remember { mutableStateOf(false) }

    val isEditing = existingCredential != null
    val darkBg    = Color(0xFF0D1117)
    val surface   = Color(0xFF161B22)
    val accent    = Color(0xFF58A6FF)

    Scaffold(
        containerColor = darkBg,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Credential" else "New Credential") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = surface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color(0xFF8B949E)
                )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VaultTextField(
                value         = site,
                onValueChange = { site = it; siteError = false },
                label         = "Site / App name",
                isError       = siteError,
                supportingText = if (siteError) "Required" else null
            )

            VaultTextField(
                value         = username,
                onValueChange = { username = it; usernameError = false },
                label         = "Username / Email",
                isError       = usernameError,
                supportingText = if (usernameError) "Required" else null
            )

            // Password field with visibility toggle
            OutlinedTextField(
                value         = password,
                onValueChange = { password = it; passwordError = false },
                label         = { Text("Password") },
                isError       = passwordError,
                supportingText = if (passwordError) {{ Text("Required") }} else null,
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                visualTransformation = if (showPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon  = {
                    Row {
                        if (onGenerate != null) {
                            IconButton(onClick = onGenerate) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Generate password",
                                    tint = Color(0xFF58A6FF)
                                )
                            }
                        }
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword)
                                    Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide" else "Show",
                                tint = Color(0xFF8B949E)
                            )
                        }
                    }
                },
                colors        = vaultTextFieldColors()
            )

            if (password.isNotEmpty()) {
                PasswordStrengthBar(
                    password = password,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value         = totpSecret,
                onValueChange = {
                    totpSecret = it
                    totpError  = it.isNotBlank() && !Base32.isValid(it)
                },
                label          = { Text("TOTP Secret (optional)") },
                isError        = totpError,
                supportingText = if (totpError) {
                    { Text("Invalid Base32 secret — check for typos") }
                } else null,
                placeholder    = { Text("e.g. JBSWY3DPEHPK3PXP", color = Color(0xFF484F58)) },
                modifier       = Modifier.fillMaxWidth(),
                singleLine     = true,
                colors         = vaultTextFieldColors()
            )

            VaultTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = "Notes (optional)",
                maxLines      = 4,
                singleLine    = false
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    siteError     = site.isBlank()
                    usernameError = username.isBlank()
                    passwordError = password.isBlank()
                    totpError     = totpSecret.isNotBlank() && !Base32.isValid(totpSecret)
                    if (siteError || usernameError || passwordError || totpError) return@Button

                    val credential = existingCredential?.copy(
                        site       = site.trim(),
                        username   = username.trim(),
                        password   = password,
                        totpSecret = totpSecret.trim(),
                        notes      = notes.trim(),
                        updatedAt  = System.currentTimeMillis()
                    ) ?: Credential(
                        site       = site.trim(),
                        username   = username.trim(),
                        password   = password,
                        totpSecret = totpSecret.trim(),
                        notes      = notes.trim()
                    )
                    onSave(credential)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Add Credential",
                    color = Color.White
                )
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────

@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean = false,
    supportingText: String? = null,
    maxLines: Int = 1,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label) },
        isError        = isError,
        supportingText = supportingText?.let { { Text(it) } },
        modifier       = Modifier.fillMaxWidth(),
        singleLine     = singleLine,
        maxLines       = maxLines,
        colors         = vaultTextFieldColors()
    )
}

@Composable
fun vaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF58A6FF),
    unfocusedBorderColor = Color(0xFF30363D),
    focusedLabelColor    = Color(0xFF58A6FF),
    unfocusedLabelColor  = Color(0xFF8B949E),
    cursorColor          = Color(0xFF58A6FF),
    focusedTextColor     = Color.White,
    unfocusedTextColor   = Color.White,
    errorBorderColor     = Color(0xFFE53935),
    errorLabelColor      = Color(0xFFE53935)
)