package com.learntogether.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

data class UserProfileUiState(
    val user: UserEntity? = null,
    val currentUser: UserEntity? = null,
    val posts: List<PostEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val selectedTab: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun load(userId: String) {
        viewModelScope.launch { userRepository.getCurrentUser().collect { u -> _uiState.update { it.copy(currentUser = u) }; u?.let { checkFollow(it.userId, userId) } } }
        viewModelScope.launch { userRepository.getUserById(userId).collect { u -> _uiState.update { it.copy(user = u, isLoading = false) } } }
        viewModelScope.launch { postRepository.getPostsByUser(userId).collect { p -> _uiState.update { it.copy(posts = p) } } }
        viewModelScope.launch { courseRepository.getCoursesByCreator(userId).collect { c -> _uiState.update { it.copy(courses = c) } } }
        viewModelScope.launch { userRepository.getFollowerCount(userId).collect { c -> _uiState.update { it.copy(followerCount = c) } } }
        viewModelScope.launch { userRepository.getFollowingCount(userId).collect { c -> _uiState.update { it.copy(followingCount = c) } } }
    }

    private fun checkFollow(currentId: String, targetId: String) {
        viewModelScope.launch { userRepository.isFollowing(currentId, targetId).collect { f -> _uiState.update { it.copy(isFollowing = f) } } }
    }

    fun toggleFollow() {
        val current = _uiState.value.currentUser?.userId ?: return
        val target = _uiState.value.user?.userId ?: return
        viewModelScope.launch {
            if (_uiState.value.isFollowing) userRepository.unfollowUser(current, target)
            else userRepository.followUser(current, target)
        }
    }

    fun selectTab(i: Int) { _uiState.update { it.copy(selectedTab = i) } }
}

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onCourseClick: (String) -> Unit,
    onEditProfile: () -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Posts", "Courses")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    val isOtherUser = uiState.currentUser != null && uiState.currentUser?.userId != userId

    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TLTopBar(
                title = uiState.user?.username ?: "User",
                onBack = onBack,
                actions = {
                    if (isOtherUser) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Report user") },
                                    onClick = {
                                        menuOpen = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Thanks — we received your report.")
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.user == null) {
            EmptyState(icon = Icons.Outlined.Person, title = "User not found", subtitle = "")
        } else {
            val user = uiState.user!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            Text(user.bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        when {
                            uiState.currentUser?.userId == userId -> {
                                OutlinedButton(onClick = onEditProfile) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit profile")
                                }
                            }
                            uiState.currentUser != null -> {
                                if (uiState.isFollowing) {
                                    OutlinedButton(
                                        onClick = { viewModel.toggleFollow() },
                                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                                    ) {
                                        Text("Unfollow")
                                    }
                                } else {
                                    Button(onClick = { viewModel.toggleFollow() }) {
                                        Text("Follow")
                                    }
                                }
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
                        if (uiState.posts.isEmpty()) item { EmptyState(icon = Icons.Outlined.Article, title = "No posts", subtitle = "") }
                        else items(uiState.posts, key = { it.postId }) { post ->
                            val postImages = MediaUrlsPartition.imageUrlsForDisplay(
                                post.imageUrls,
                                post.videoUrl,
                                post.audioUrl
                            )
                            PostCard(
                                authorName = user.username,
                                authorHandle = user.handle,
                                authorImageUrl = user.profilePictureUrl.ifBlank { null },
                                title = post.title,
                                content = post.content,
                                likeCount = post.likeCount,
                                commentCount = post.commentCount,
                                isLiked = false,
                                hasImageAttachments = postImages.isNotEmpty(),
                                hasVideoAttachments = MediaUrlsPartition.hasClassifiedVideoUrls(post.videoUrl),
                                hasAudioAttachments = MediaUrlsPartition.hasClassifiedAudioUrls(post.audioUrl),
                                imageUrl = postImages.firstOrNull(),
                                timeAgo = TimeUtils.getTimeAgo(post.createdAt),
                                onLikeClick = {},
                                onClick = { onPostClick(post.postId) },
                                onAuthorClick = {}
                            )
                        }
                    }
                    1 -> {
                        if (uiState.courses.isEmpty()) item { EmptyState(icon = Icons.Outlined.MenuBook, title = "No courses", subtitle = "") }
                        else items(uiState.courses, key = { it.courseId }) { course ->
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
        }
    }
}
