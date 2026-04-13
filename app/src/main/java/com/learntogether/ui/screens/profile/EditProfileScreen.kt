@file:OptIn(ExperimentalMaterial3Api::class)

package com.learntogether.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.repository.UserRepository
import com.learntogether.ui.components.TLTopBar
import com.learntogether.ui.components.UserAvatar
import com.learntogether.util.displayHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val StatusQuickEmojis = listOf(
    "😀", "😃", "😄", "😁", "😅", "😂", "🤣", "😊", "🙂", "😉",
    "😍", "🥰", "😎", "🤓", "🧐", "🤔", "😴", "🤯", "🫡", "👋",
    "👍", "👎", "🙏", "💪", "🔥", "✨", "💯", "❤️", "🧡", "💙",
    "📚", "✏️", "📝", "🎓", "🏫", "💻", "🖥️", "📱", "🎮", "🎧",
    "☕", "🍕", "🌙", "🌟", "⚡", "🎯", "✅", "❌", "❓", "💬"
)

data class EditProfileUiState(
    val profilePicturePathOrUrl: String = "",
    val bio: String = "",
    val status: String = "",
    val displayName: String = "",
    val handleLabel: String = "",
    val error: String? = null,
    val saving: Boolean = false,
    val importingImage: Boolean = false,
    val saved: Boolean = false,
    val ready: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser().filterNotNull().first()
            _uiState.update {
                it.copy(
                    profilePicturePathOrUrl = user.profilePictureUrl,
                    bio = user.bio,
                    status = user.status,
                    displayName = user.username,
                    handleLabel = displayHandle(user.handle),
                    ready = true
                )
            }
        }
    }

    fun updateBio(v: String) = _uiState.update { it.copy(bio = v, error = null) }

    fun updateStatus(v: String) {
        if (v.length <= 100) _uiState.update { it.copy(status = v, error = null) }
    }

    fun appendStatusEmoji(emoji: String) {
        _uiState.update { s ->
            val next = s.status + emoji
            if (next.length <= 100) s.copy(status = next, error = null)
            else s.copy(error = "Status is at most 100 characters")
        }
    }

    fun onProfileImagePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(importingImage = true, error = null) }
            userRepository.importProfileImageFromUri(uri).fold(
                onSuccess = { path ->
                    _uiState.update { it.copy(profilePicturePathOrUrl = path, importingImage = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            importingImage = false,
                            error = e.message ?: "Could not import image"
                        )
                    }
                }
            )
        }
    }

    fun clearProfilePhoto() {
        _uiState.update { it.copy(profilePicturePathOrUrl = "", error = null) }
    }

    fun save() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val result = userRepository.updateCurrentUserProfileFields(
                profilePictureUrl = s.profilePicturePathOrUrl,
                bio = s.bio,
                status = s.status
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(saving = false, saved = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(saving = false, error = e.message ?: "Could not save profile")
                    }
                }
            )
        }
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEmojiPicker by remember { mutableStateOf(false) }
    val emojiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickImage = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onProfileImagePicked(it) }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onBack()
        }
    }

    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            sheetState = emojiSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    "Pick an emoji",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 52.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(StatusQuickEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.appendStatusEmoji(emoji)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TLTopBar(
                title = "Edit profile",
                onBack = onBack
            )
        }
    ) { padding ->
        if (!uiState.ready) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                UserAvatar(
                    imageUrl = uiState.profilePicturePathOrUrl.ifBlank { null },
                    username = uiState.displayName,
                    size = 96
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                    enabled = !uiState.importingImage && !uiState.saving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.importingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose photo")
                }
                if (uiState.profilePicturePathOrUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { viewModel.clearProfilePhoto() },
                        enabled = !uiState.importingImage && !uiState.saving
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Text(
                "Uses your device photo library (no URL). Save to keep changes in the database.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
            )
            OutlinedTextField(
                value = uiState.handleLabel,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Handle") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) }
            )
            Text(
                "Change name or handle in Settings > Account.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            OutlinedTextField(
                value = uiState.status,
                onValueChange = viewModel::updateStatus,
                label = { Text("Status") },
                placeholder = { Text("e.g. 📚 Studying · 🎮 Gaming") },
                supportingText = { Text("${uiState.status.length}/100 · Tap the emoji icon") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 2,
                leadingIcon = { Icon(Icons.Outlined.Mood, contentDescription = null) },
                trailingIcon = {
                    IconButton(
                        onClick = { showEmojiPicker = true },
                        enabled = !uiState.saving
                    ) {
                        Icon(
                            Icons.Outlined.EmojiEmotions,
                            contentDescription = "Open emoji picker"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = uiState.bio,
                onValueChange = viewModel::updateBio,
                label = { Text("Bio") },
                placeholder = { Text("Tell others what you study or teach…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp),
                minLines = 4
            )

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.saving && !uiState.importingImage
            ) {
                if (uiState.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save profile")
                }
            }
        }
    }
}
