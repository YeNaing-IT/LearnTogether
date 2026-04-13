package com.learntogether.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.repository.*
import com.learntogether.ui.components.*
import com.learntogether.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val completedCourses: Int = 0,
    val ongoingCourses: Int = 0,
    val likedPosts: Int = 0,
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val totalLearnedMinutes: Int = 0,
    val pomodoroMinutes: Int = 0,
    val pomodoroSessions: Int = 0,
    val postsCreated: Int = 0,
    val coursesCreated: Int = 0,
    val challengesCompleted: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val postRepository: PostRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val challengeRepository: ChallengeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.update { it.copy(totalLearnedMinutes = user.totalLearnedMinutes, isLoading = false) }
                    loadStats(user.userId)
                }
            }
        }
    }

    private fun loadStats(userId: String) {
        viewModelScope.launch { courseRepository.getCompletedCourseCount(userId).collect { v -> _uiState.update { it.copy(completedCourses = v) } } }
        viewModelScope.launch { courseRepository.getOngoingCourseCount(userId).collect { v -> _uiState.update { it.copy(ongoingCourses = v) } } }
        viewModelScope.launch { postRepository.getLikedPostCount(userId).collect { v -> _uiState.update { it.copy(likedPosts = v) } } }
        viewModelScope.launch { userRepository.getFollowingCount(userId).collect { v -> _uiState.update { it.copy(followingCount = v) } } }
        viewModelScope.launch { userRepository.getFollowerCount(userId).collect { v -> _uiState.update { it.copy(followerCount = v) } } }
        viewModelScope.launch { pomodoroRepository.getTotalMinutes(userId).collect { v -> _uiState.update { it.copy(pomodoroMinutes = v ?: 0) } } }
        viewModelScope.launch { pomodoroRepository.getSessionCount(userId).collect { v -> _uiState.update { it.copy(pomodoroSessions = v) } } }
        viewModelScope.launch { postRepository.getPostCount(userId).collect { v -> _uiState.update { it.copy(postsCreated = v) } } }
        viewModelScope.launch { courseRepository.getCourseCount(userId).collect { v -> _uiState.update { it.copy(coursesCreated = v) } } }
        viewModelScope.launch { challengeRepository.getCompletedChallengeCount(userId).collect { v -> _uiState.update { it.copy(challengesCompleted = v) } } }
    }
}

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TLTopBar(title = "Statistics", onBack = onBack) }) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Learning Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(icon = Icons.Outlined.Schedule, label = "Total Learned", value = TimeUtils.formatMinutes(uiState.totalLearnedMinutes + uiState.pomodoroMinutes),
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                    StatCard(icon = Icons.Outlined.Timer, label = "Pomodoro", value = TimeUtils.formatMinutes(uiState.pomodoroMinutes),
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
                }

                Text("Courses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(icon = Icons.Outlined.CheckCircle, label = "Completed", value = "${uiState.completedCourses}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                    StatCard(icon = Icons.Outlined.PlayCircle, label = "Ongoing", value = "${uiState.ongoingCourses}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
                    StatCard(icon = Icons.Outlined.Create, label = "Created", value = "${uiState.coursesCreated}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.tertiary)
                }

                Text("Social", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(icon = Icons.Outlined.Favorite, label = "Liked Posts", value = "${uiState.likedPosts}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.tertiary)
                    StatCard(icon = Icons.Outlined.People, label = "Following", value = "${uiState.followingCount}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                    StatCard(icon = Icons.Outlined.PersonAdd, label = "Followers", value = "${uiState.followerCount}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
                }

                Text("Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(icon = Icons.Outlined.Article, label = "Posts", value = "${uiState.postsCreated}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                    StatCard(icon = Icons.Outlined.EmojiEvents, label = "Challenges", value = "${uiState.challengesCompleted}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
                    StatCard(icon = Icons.Outlined.Timer, label = "Sessions", value = "${uiState.pomodoroSessions}",
                        modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}
