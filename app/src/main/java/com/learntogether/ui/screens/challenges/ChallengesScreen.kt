package com.learntogether.ui.screens.challenges

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.ChallengeEntity
import com.learntogether.data.repository.ChallengeRepository
import com.learntogether.data.repository.UserRepository
import com.learntogether.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChallengesUiState(
    val todayChallenges: List<ChallengeEntity> = emptyList(),
    val preDesigned: List<Pair<String, String>> = emptyList(),
    val customTitle: String = "",
    val customDescription: String = "",
    val showAddDialog: Boolean = false,
    val showPreDesigned: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChallengesUiState())
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(preDesigned = challengeRepository.getPreDesignedTemplates()) }
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    challengeRepository.getTodayChallenges(user.userId).collect { challenges ->
                        _uiState.update { it.copy(todayChallenges = challenges, isLoading = false) }
                    }
                }
            }
        }
    }

    fun toggleComplete(challenge: ChallengeEntity) {
        viewModelScope.launch { challengeRepository.toggleChallengeComplete(challenge) }
    }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideAddDialog() { _uiState.update { it.copy(showAddDialog = false, customTitle = "", customDescription = "") } }
    fun showPreDesigned() { _uiState.update { it.copy(showPreDesigned = !it.showPreDesigned) } }
    fun updateTitle(t: String) { _uiState.update { it.copy(customTitle = t) } }
    fun updateDescription(d: String) { _uiState.update { it.copy(customDescription = d) } }

    fun addCustomChallenge() {
        val s = _uiState.value
        if (s.customTitle.isBlank()) return
        viewModelScope.launch {
            val user = userRepository.getCurrentUserOnce() ?: return@launch
            challengeRepository.createChallenge(user.userId, s.customTitle, s.customDescription)
            hideAddDialog()
        }
    }

    fun addPreDesignedChallenge(title: String, desc: String) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUserOnce() ?: return@launch
            challengeRepository.createChallenge(user.userId, title, desc, isPreDesigned = true)
        }
    }

    fun deleteChallenge(challenge: ChallengeEntity) {
        viewModelScope.launch { challengeRepository.deleteChallenge(challenge) }
    }
}

@Composable
fun ChallengesScreen(
    onBack: () -> Unit,
    viewModel: ChallengesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Add dialog
    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddDialog() },
            title = { Text("New Challenge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = uiState.customTitle, onValueChange = viewModel::updateTitle, label = { Text("Challenge Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = uiState.customDescription, onValueChange = viewModel::updateDescription, label = { Text("Description (optional)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.addCustomChallenge() }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { viewModel.hideAddDialog() }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TLTopBar(title = "Daily Challenges", onBack = onBack, actions = {
                IconButton(onClick = { viewModel.showAddDialog() }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today's summary
            item {
                val completed = uiState.todayChallenges.count { it.isCompleted }
                val total = uiState.todayChallenges.size
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Today's Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (total > 0) {
                            LinearProgressIndicator(
                                progress = { completed.toFloat() / total },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$completed of $total completed", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("No challenges set for today. Add some below!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Today's challenges
            item { Text("My Challenges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

            if (uiState.todayChallenges.isEmpty()) {
                item { EmptyState(icon = Icons.Outlined.EmojiEvents, title = "No challenges today", subtitle = "Add a custom challenge or pick from pre-designed ones") }
            } else {
                items(uiState.todayChallenges, key = { it.challengeId }) { challenge ->
                    val bgColor by animateColorAsState(
                        if (challenge.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface, label = "bg"
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = challenge.isCompleted, onCheckedChange = { viewModel.toggleComplete(challenge) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    challenge.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (challenge.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (challenge.description.isNotBlank()) {
                                    Text(challenge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteChallenge(challenge) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Pre-designed challenges
            item {
                OutlinedButton(onClick = { viewModel.showPreDesigned() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(if (uiState.showPreDesigned) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pre-designed Challenges")
                }
            }

            if (uiState.showPreDesigned) {
                items(uiState.preDesigned) { (title, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.addPreDesignedChallenge(title, desc) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
