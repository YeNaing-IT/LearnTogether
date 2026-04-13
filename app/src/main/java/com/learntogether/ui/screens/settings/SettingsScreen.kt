package com.learntogether.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.learntogether.ui.components.TLTopBar
import com.learntogether.ui.theme.AppAccentPalette
import com.learntogether.ui.theme.AppFontStyle
import com.learntogether.util.displayHandle

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFontMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.accountSuccessMessage) {
        val msg = uiState.accountSuccessMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeAccountSuccessMessage()
    }

    when (uiState.accountDialog) {
        AccountDialog.ChangeEmail -> AccountTextFieldDialog(
            title = "Change email",
            confirmLabel = "Save",
            fields = listOf(
                AccountField("New email", AccountFieldType.Email),
                AccountField("Current password", AccountFieldType.Password)
            ),
            inProgress = uiState.accountActionInProgress,
            errorMessage = uiState.accountFormError,
            onDismiss = { viewModel.dismissAccountDialog() },
            onConfirm = { values ->
                viewModel.submitEmailChange(values[0], values[1])
            }
        )
        AccountDialog.ChangeUsername -> AccountTextFieldDialog(
            title = "Change display name",
            confirmLabel = "Save",
            fields = listOf(
                AccountField("Display name", AccountFieldType.Plain),
                AccountField("Current password", AccountFieldType.Password)
            ),
            inProgress = uiState.accountActionInProgress,
            errorMessage = uiState.accountFormError,
            onDismiss = { viewModel.dismissAccountDialog() },
            onConfirm = { values ->
                viewModel.submitUsernameChange(values[0], values[1])
            }
        )
        AccountDialog.ChangeHandle -> AccountTextFieldDialog(
            title = "Change handle",
            confirmLabel = "Save",
            fields = listOf(
                AccountField("New @handle", AccountFieldType.Plain),
                AccountField("Current password", AccountFieldType.Password)
            ),
            inProgress = uiState.accountActionInProgress,
            errorMessage = uiState.accountFormError,
            onDismiss = { viewModel.dismissAccountDialog() },
            onConfirm = { values ->
                viewModel.submitHandleChange(values[0], values[1])
            }
        )
        AccountDialog.DeleteAccount -> AccountTextFieldDialog(
            title = "Delete account",
            confirmLabel = "Delete permanently",
            destructiveConfirm = true,
            fields = listOf(AccountField("Password", AccountFieldType.Password)),
            inProgress = uiState.accountActionInProgress,
            errorMessage = uiState.accountFormError,
            supportingText = "This removes your profile, posts, courses you created, and other data on this device. This cannot be undone.",
            onDismiss = { viewModel.dismissAccountDialog() },
            onConfirm = { values ->
                viewModel.submitDeleteAccount(values[0], onLogout)
            }
        )
        AccountDialog.None -> Unit
    }

    if (uiState.showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideUpdateDialog() },
            title = { Text("Check for Updates") },
            text = { Text("You are running the latest version of LearnTogether (v1.0.0). No updates available.") },
            confirmButton = { TextButton(onClick = { viewModel.hideUpdateDialog() }) { Text("OK") } }
        )
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideLogoutDialog() },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    viewModel.hideLogoutDialog()
                    onLogout()
                }) { Text("Logout", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { viewModel.hideLogoutDialog() }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = { TLTopBar(title = "Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            uiState.currentUser?.let { user ->
                AccountIdentityHeader(
                    displayName = user.username,
                    handle = user.handle,
                    email = user.email
                )

                Spacer(modifier = Modifier.height(8.dp))

                AccountActionRow(
                    icon = Icons.Outlined.Email,
                    title = "Change email",
                    subtitle = "Update the email you use to sign in",
                    onClick = { viewModel.openAccountDialog(AccountDialog.ChangeEmail) }
                )
                AccountActionRow(
                    icon = Icons.Outlined.Person,
                    title = "Change display name",
                    subtitle = "How your name appears to others",
                    onClick = { viewModel.openAccountDialog(AccountDialog.ChangeUsername) }
                )
                AccountActionRow(
                    icon = Icons.Outlined.AlternateEmail,
                    title = "Change handle",
                    subtitle = "Your unique @name",
                    onClick = { viewModel.openAccountDialog(AccountDialog.ChangeHandle) }
                )
            } ?: run {
                Text(
                    "Sign in to manage your account.",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                        Text("Switch between light and dark theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = uiState.isDarkMode, onCheckedChange = { viewModel.toggleDarkMode() })
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Card(shape = RoundedCornerShape(12.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showFontMenu = !showFontMenu }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.TextFormat, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Font Style", style = MaterialTheme.typography.bodyLarge)
                            Text("Current: ${uiState.fontStyle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (showFontMenu) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    }
                    if (showFontMenu) {
                        AppFontStyle.entries.forEach { style ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.setFontStyle(style.displayName)
                                    showFontMenu = false
                                }.padding(horizontal = 56.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.fontStyle == style.displayName,
                                    onClick = {
                                        viewModel.setFontStyle(style.displayName)
                                        showFontMenu = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(style.displayName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Card(shape = RoundedCornerShape(12.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Accent color", style = MaterialTheme.typography.bodyLarge)
                            Text("Primary theme color", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppAccentPalette.entries.toList()) { palette ->
                            FilterChip(
                                selected = uiState.accentPaletteKey == palette.persistenceKey,
                                onClick = { viewModel.setAccentPalette(palette.persistenceKey) },
                                label = { Text(palette.displayName) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "About",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.showUpdateDialog() }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Check for Updates", style = MaterialTheme.typography.bodyLarge)
                        Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.currentUser != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.openAccountDialog(AccountDialog.DeleteAccount) }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Delete account", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                            Text("Remove your data from this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.showLogoutDialog() }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Logout", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AccountIdentityHeader(
    displayName: String,
    handle: String,
    email: String
) {
    var emailVisible by rememberSaveable { mutableStateOf(false) }
    val handleDisplay = displayHandle(handle)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Sign in as",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Handle",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                handleDisplay,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { emailVisible = !emailVisible }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Email",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (emailVisible) email else obscuredEmail(email),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (emailVisible) "Tap to hide" else "Tap to show full email",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (emailVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (emailVisible) "Hide email" else "Show email",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun obscuredEmail(email: String): String {
    val at = email.indexOf('@')
    if (at < 1) return "••••••••"
    val local = email.substring(0, at)
    val domain = email.substring(at + 1)
    if (domain.isEmpty()) return "••••••••"
    val maskedLocal = when {
        local.length <= 1 -> "•••"
        else -> {
            val dots = minOf(5, local.length - 1).coerceAtLeast(2)
            "${local.first()}${"•".repeat(dots)}"
        }
    }
    return "$maskedLocal@$domain"
}

@Composable
private fun AccountActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private enum class AccountFieldType { Plain, Email, Password }

private data class AccountField(val label: String, val type: AccountFieldType)

@Composable
private fun AccountTextFieldDialog(
    title: String,
    confirmLabel: String,
    fields: List<AccountField>,
    inProgress: Boolean,
    errorMessage: String?,
    destructiveConfirm: Boolean = false,
    supportingText: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val values = remember(title, fields.size) { List(fields.size) { mutableStateOf("") } }

    AlertDialog(
        onDismissRequest = { if (!inProgress) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = values[index].value,
                        onValueChange = { values[index].value = it },
                        label = { Text(field.label) },
                        singleLine = true,
                        enabled = !inProgress,
                        visualTransformation = if (field.type == AccountFieldType.Password) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when (field.type) {
                                AccountFieldType.Email -> KeyboardType.Email
                                AccountFieldType.Password -> KeyboardType.Password
                                else -> KeyboardType.Text
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(values.map { it.value }) },
                enabled = !inProgress
            ) {
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        confirmLabel,
                        color = if (destructiveConfirm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !inProgress) { Text("Cancel") }
        }
    )
}
