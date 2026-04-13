package com.learntogether.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.PostEntity
import com.learntogether.data.local.entity.UserEntity
import com.learntogether.data.repository.PostRepository
import com.learntogether.data.repository.UserRepository
import com.learntogether.data.repository.EducationApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val currentUser: UserEntity? = null,
    val feedPosts: List<PostEntity> = emptyList(),
    val latestPosts: List<PostEntity> = emptyList(),
    val userCache: Map<String, UserEntity> = emptyMap(),
    val likedPosts: Set<String> = emptySet(),
    val selectedTab: Int = 0,  // 0 = Following, 1 = Latest
    val dailyQuote: String = "",
    val quoteAuthor: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val educationApiRepository: EducationApiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val userObserveJobs = mutableMapOf<String, Job>()

    init {
        loadFeed()
        loadDailyQuote()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    loadFeedPosts(user.userId)
                    loadLatestPosts()
                }
            }
        }
    }

    private fun loadFeedPosts(userId: String) {
        viewModelScope.launch {
            postRepository.getFeedPosts(userId).collect { posts ->
                _uiState.update { it.copy(feedPosts = posts, isLoading = false) }
                // Cache user data for each post author
                posts.map { it.authorId }.distinct().forEach { authorId ->
                    loadUserToCache(authorId)
                }
            }
        }
    }

    private fun loadLatestPosts() {
        viewModelScope.launch {
            postRepository.getAllPosts().collect { posts ->
                _uiState.update { it.copy(latestPosts = posts, isLoading = false) }
                posts.map { it.authorId }.distinct().forEach { authorId ->
                    loadUserToCache(authorId)
                }
            }
        }
    }

    private fun loadUserToCache(userId: String) {
        if (userObserveJobs.containsKey(userId)) return
        userObserveJobs[userId] = viewModelScope.launch {
            userRepository.getUserById(userId).collect { user ->
                _uiState.update { state ->
                    val next = if (user != null) state.userCache + (userId to user) else state.userCache - userId
                    state.copy(userCache = next)
                }
            }
        }
    }

    private fun loadDailyQuote() {
        viewModelScope.launch {
            val result = educationApiRepository.getDailyQuote()
            result.onSuccess { quote ->
                _uiState.update { it.copy(dailyQuote = quote.q, quoteAuthor = quote.a) }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleLike(postId: String) {
        val userId = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch {
            val isCurrentlyLiked = _uiState.value.likedPosts.contains(postId)
            if (isCurrentlyLiked) {
                postRepository.unlikePost(userId, postId)
                _uiState.update { it.copy(likedPosts = it.likedPosts - postId) }
            } else {
                postRepository.likePost(userId, postId)
                _uiState.update { it.copy(likedPosts = it.likedPosts + postId) }
            }
        }
    }
}
