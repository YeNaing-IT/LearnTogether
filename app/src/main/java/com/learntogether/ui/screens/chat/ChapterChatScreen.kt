package com.learntogether.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.ChatMessageEntity
import com.learntogether.data.local.entity.UserEntity
import com.learntogether.data.repository.CourseRepository
import com.learntogether.data.repository.UserRepository
import com.learntogether.ui.components.*
import com.learntogether.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val userCache: Map<String, UserEntity> = emptyMap(),
    val currentUser: UserEntity? = null,
    val messageText: String = "",
    val chapterTitle: String = ""
)

@HiltViewModel
class ChapterChatViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val senderObserveJobs = mutableMapOf<String, Job>()

    fun load(chapterId: String) {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
        viewModelScope.launch {
            courseRepository.getChapterById(chapterId).collect { chapter ->
                _uiState.update { it.copy(chapterTitle = chapter?.title ?: "Discussion") }
            }
        }
        viewModelScope.launch {
            courseRepository.getChatMessages(chapterId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
                messages.map { it.senderId }.distinct().forEach { id -> loadUser(id) }
            }
        }
    }

    private fun loadUser(userId: String) {
        if (senderObserveJobs.containsKey(userId)) return
        senderObserveJobs[userId] = viewModelScope.launch {
            userRepository.getUserById(userId).collect { user ->
                _uiState.update { state ->
                    val next = if (user != null) state.userCache + (userId to user) else state.userCache - userId
                    state.copy(userCache = next)
                }
            }
        }
    }

    fun updateMessage(text: String) { _uiState.update { it.copy(messageText = text) } }

    fun sendMessage(chapterId: String) {
        val text = _uiState.value.messageText.trim()
        val userId = _uiState.value.currentUser?.userId ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            courseRepository.sendChatMessage(chapterId, userId, text)
            _uiState.update { it.copy(messageText = "") }
        }
    }
}

@Composable
fun ChapterChatScreen(
    chapterId: String,
    onBack: () -> Unit,
    viewModel: ChapterChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(chapterId) { viewModel.load(chapterId) }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.lastIndex)
    }

    Scaffold(
        topBar = { TLTopBar(title = uiState.chapterTitle, onBack = onBack) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.messageText, onValueChange = viewModel::updateMessage,
                        placeholder = { Text("Ask a question...") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.sendMessage(chapterId) }, enabled = uiState.messageText.isNotBlank()) {
                        Icon(Icons.Filled.Send, contentDescription = "Send",
                            tint = if (uiState.messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(icon = Icons.Outlined.Forum, title = "No messages yet", subtitle = "Start the discussion!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.messageId }) { msg ->
                    val sender = uiState.userCache[msg.senderId]
                    val isMe = msg.senderId == uiState.currentUser?.userId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isMe) {
                            UserAvatar(imageUrl = sender?.profilePictureUrl, username = sender?.username ?: "?", size = 32)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (!isMe) {
                                    Text(sender?.username ?: "Unknown", style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(msg.content, style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(TimeUtils.getTimeAgo(msg.createdAt), style = MaterialTheme.typography.labelSmall,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
