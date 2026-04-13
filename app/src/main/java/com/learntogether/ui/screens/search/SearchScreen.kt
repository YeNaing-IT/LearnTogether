package com.learntogether.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.*
import com.learntogether.data.repository.*
import com.learntogether.ui.components.*
import com.learntogether.util.MediaUrlsPartition
import com.learntogether.util.Categories
import com.learntogether.util.TimeUtils
import com.learntogether.util.displayHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val posts: List<PostEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val postAuthors: Map<String, UserEntity> = emptyMap(),
    val likedPostIds: Set<String> = emptySet(),
    val currentUser: UserEntity? = null,
    val selectedTab: Int = 0,
    val isSearching: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val authorObserveJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun updateQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (q.length < 2 && _uiState.value.selectedCategory == null) {
                _uiState.update {
                    it.copy(posts = emptyList(), courses = emptyList(), users = emptyList(), isSearching = false)
                }
                return@launch
            }
            search()
        }
    }

    fun selectCategory(cat: String?) {
        _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == cat) null else cat) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { search() }
    }

    fun selectTab(i: Int) {
        _uiState.update { it.copy(selectedTab = i) }
    }

    fun toggleLike(postId: String) {
        val userId = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch {
            val liked = _uiState.value.likedPostIds.contains(postId)
            if (liked) {
                postRepository.unlikePost(userId, postId)
                _uiState.update { s ->
                    s.copy(
                        likedPostIds = s.likedPostIds - postId,
                        posts = s.posts.map { p ->
                            if (p.postId == postId) p.copy(likeCount = (p.likeCount - 1).coerceAtLeast(0)) else p
                        }
                    )
                }
            } else {
                postRepository.likePost(userId, postId)
                _uiState.update { s ->
                    s.copy(
                        likedPostIds = s.likedPostIds + postId,
                        posts = s.posts.map { p ->
                            if (p.postId == postId) p.copy(likeCount = p.likeCount + 1) else p
                        }
                    )
                }
            }
        }
    }

    private suspend fun search() {
        val q = _uiState.value.query
        val cat = _uiState.value.selectedCategory
        val trimmedQ = q.trim()
        val searchTerm = if (cat != null && trimmedQ.isEmpty()) cat else trimmedQ

        if (searchTerm.isBlank()) {
            _uiState.update { it.copy(posts = emptyList(), courses = emptyList(), users = emptyList(), isSearching = false) }
            return
        }

        _uiState.update { it.copy(isSearching = true) }
        val me = _uiState.value.currentUser
        val posts = postRepository.searchPosts(searchTerm)
        val courses = courseRepository.searchCourses(searchTerm)
        val userSearchInput = when {
            trimmedQ.startsWith("@") -> trimmedQ.trimStart { it == '@' }.trim().takeIf { it.isNotEmpty() }
            trimmedQ.isNotEmpty() -> trimmedQ
            else -> searchTerm.takeIf { it.isNotBlank() }
        }
        val users = if (userSearchInput.isNullOrBlank()) emptyList() else userRepository.searchUsers(userSearchInput)

        val liked = if (me != null && posts.isNotEmpty()) {
            postRepository.getLikedPostIdsForUser(me.userId, posts.map { it.postId })
        } else emptySet()

        authorObserveJobs.values.forEach { it.cancel() }
        authorObserveJobs.clear()
        posts.map { it.authorId }.distinct().forEach { loadPostAuthor(it) }

        _uiState.update {
            it.copy(
                posts = posts,
                courses = courses,
                users = users,
                likedPostIds = liked,
                isSearching = false
            )
        }
    }

    private fun loadPostAuthor(authorId: String) {
        if (authorObserveJobs.containsKey(authorId)) return
        authorObserveJobs[authorId] = viewModelScope.launch {
            userRepository.getUserById(authorId).collect { user ->
                _uiState.update { s ->
                    val next = if (user != null) s.postAuthors + (authorId to user) else s.postAuthors - authorId
                    s.copy(postAuthors = next)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onPostClick: (String) -> Unit,
    onCourseClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Courses", "Posts", "Users")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::updateQuery,
                    placeholder = { Text("Search courses, posts, @users...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            item {
                TabRow(selectedTabIndex = uiState.selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEachIndexed { i, t ->
                        Tab(
                            selected = uiState.selectedTab == i,
                            onClick = { viewModel.selectTab(i) },
                            text = { Text(t, fontWeight = if (uiState.selectedTab == i) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            if (uiState.selectedTab == 0) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(Categories.all) { cat ->
                            FilterChip(
                                selected = uiState.selectedCategory == cat,
                                onClick = { viewModel.selectCategory(cat) },
                                label = { Text(cat, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }

            if (uiState.isSearching) {
                item { LoadingIndicator(modifier = Modifier.padding(32.dp)) }
            } else {
                when (uiState.selectedTab) {
                    0 -> {
                        if (uiState.courses.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.MenuBook,
                                    title = "No courses found",
                                    subtitle = "Try a different search"
                                )
                            }
                        } else {
                            items(uiState.courses, key = { it.courseId }) { course ->
                                CourseCard(
                                    title = course.title,
                                    description = course.description,
                                    category = course.category,
                                    creatorName = "",
                                    enrollmentCount = course.enrollmentCount,
                                    durationDays = course.durationDays,
                                    imageUrl = MediaUrlsPartition.firstCommaSeparatedUrl(course.imageUrl),
                                    isPrivate = course.isPrivate,
                                    onClick = { onCourseClick(course.courseId) }
                                )
                            }
                        }
                    }
                    1 -> {
                        if (uiState.posts.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.Article,
                                    title = "No posts found",
                                    subtitle = "Try a different search"
                                )
                            }
                        } else {
                            items(uiState.posts, key = { it.postId }) { post ->
                                val author = uiState.postAuthors[post.authorId]
                                val postDisplayImages = MediaUrlsPartition.imageUrlsForDisplay(
                                    post.imageUrls,
                                    post.videoUrl,
                                    post.audioUrl
                                )
                                PostCard(
                                    authorName = author?.username ?: "Unknown",
                                    authorHandle = author?.handle ?: "",
                                    authorImageUrl = author?.profilePictureUrl?.ifBlank { null },
                                    title = post.title,
                                    content = post.content,
                                    likeCount = post.likeCount,
                                    commentCount = post.commentCount,
                                    isLiked = uiState.likedPostIds.contains(post.postId),
                                    hasImageAttachments = postDisplayImages.isNotEmpty(),
                                    hasVideoAttachments = MediaUrlsPartition.hasClassifiedVideoUrls(post.videoUrl),
                                    hasAudioAttachments = MediaUrlsPartition.hasClassifiedAudioUrls(post.audioUrl),
                                    imageUrl = postDisplayImages.firstOrNull(),
                                    timeAgo = TimeUtils.getTimeAgo(post.createdAt),
                                    onLikeClick = { viewModel.toggleLike(post.postId) },
                                    onClick = { onPostClick(post.postId) },
                                    onAuthorClick = { author?.userId?.let(onUserClick) }
                                )
                            }
                        }
                    }
                    2 -> {
                        if (uiState.users.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Outlined.People,
                                    title = "No users found",
                                    subtitle = "Try @username"
                                )
                            }
                        } else {
                            items(uiState.users, key = { it.userId }) { user ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { onUserClick(user.userId) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            imageUrl = user.profilePictureUrl.ifBlank { null },
                                            username = user.username,
                                            size = 42
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                user.username,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                displayHandle(user.handle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (user.status.isNotBlank()) {
                                                Text(
                                                    user.status,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
