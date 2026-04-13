package com.learntogether.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.*
import com.learntogether.data.repository.*
import com.learntogether.ui.components.*
import com.learntogether.util.MediaUrlsPartition
import com.learntogether.util.TimeUtils
import com.learntogether.util.displayHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserEntity? = null,
    val posts: List<PostEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val selectedTab: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
                if (user != null) {
                    loadUserData(user.userId)
                }
            }
        }
    }

    private fun loadUserData(userId: String) {
        viewModelScope.launch { postRepository.getPostsByUser(userId).collect { posts -> _uiState.update { it.copy(posts = posts) } } }
        viewModelScope.launch { courseRepository.getCoursesByCreator(userId).collect { courses -> _uiState.update { it.copy(courses = courses) } } }
        viewModelScope.launch { userRepository.getFollowerCount(userId).collect { count -> _uiState.update { it.copy(followerCount = count) } } }
        viewModelScope.launch { userRepository.getFollowingCount(userId).collect { count -> _uiState.update { it.copy(followingCount = count) } } }
    }

    fun selectTab(i: Int) { _uiState.update { it.copy(selectedTab = i) } }

    fun logout() {
        viewModelScope.launch { userRepository.logout() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onPostClick: (String) -> Unit,
    onCourseClick: (String) -> Unit,
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Posts", "Courses")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onStatisticsClick) { Icon(Icons.Outlined.BarChart, contentDescription = "Statistics") }
                    IconButton(onClick = onEditProfile) { Icon(Icons.Outlined.Edit, contentDescription = "Edit profile") }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Not logged in", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToLogin) { Text("Sign In") }
                }
            }
        } else {
            val user = uiState.user!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar + info
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        UserAvatar(imageUrl = user.profilePictureUrl.ifBlank { null }, username = user.username, size = 80)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(user.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(displayHandle(user.handle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (user.status.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    user.status,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        if (user.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(user.bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${uiState.followerCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Followers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${uiState.followingCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Following", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${uiState.posts.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Posts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item {
                    TabRow(selectedTabIndex = uiState.selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEachIndexed { index, title ->
                            Tab(selected = uiState.selectedTab == index, onClick = { viewModel.selectTab(index) },
                                text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) })
                        }
                    }
                }

                when (uiState.selectedTab) {
                    0 -> {
                        if (uiState.posts.isEmpty()) {
                            item { EmptyState(icon = Icons.Outlined.Article, title = "No posts yet", subtitle = "Share your knowledge!") }
                        } else {
                            items(uiState.posts, key = { it.postId }) { post ->
                                val postImages = MediaUrlsPartition.imageUrlsForDisplay(
                                    post.imageUrls,
                                    post.videoUrl,
                                    post.audioUrl
                                )
                                PostCard(
                                    authorName = user.username, authorHandle = user.handle, authorImageUrl = user.profilePictureUrl.ifBlank { null },
                                    title = post.title, content = post.content, likeCount = post.likeCount, commentCount = post.commentCount,
                                    isLiked = false,
                                    hasImageAttachments = postImages.isNotEmpty(),
                                    hasVideoAttachments = MediaUrlsPartition.hasClassifiedVideoUrls(post.videoUrl),
                                    hasAudioAttachments = MediaUrlsPartition.hasClassifiedAudioUrls(post.audioUrl),
                                    imageUrl = postImages.firstOrNull(),
                                    timeAgo = TimeUtils.getTimeAgo(post.createdAt),
                                    onLikeClick = {}, onClick = { onPostClick(post.postId) }, onAuthorClick = {}
                                )
                            }
                        }
                    }
                    1 -> {
                        if (uiState.courses.isEmpty()) {
                            item { EmptyState(icon = Icons.Outlined.MenuBook, title = "No courses yet", subtitle = "Create your first course!") }
                        } else {
                            items(uiState.courses, key = { it.courseId }) { course ->
                                CourseCard(
                                    title = course.title,
                                    description = course.description,
                                    category = course.category,
                                    creatorName = user.username,
                                    enrollmentCount = course.enrollmentCount,
                                    durationDays = course.durationDays,
                                    imageUrl = MediaUrlsPartition.firstCommaSeparatedUrl(course.imageUrl),
                                    isPrivate = course.isPrivate,
                                    onClick = { onCourseClick(course.courseId) }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
