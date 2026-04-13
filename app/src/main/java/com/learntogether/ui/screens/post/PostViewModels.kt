package com.learntogether.ui.screens.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.CommentEntity
import com.learntogether.data.local.entity.PostEntity
import com.learntogether.data.local.entity.UserEntity
import com.learntogether.data.repository.PostRepository
import com.learntogether.data.repository.UserRepository
import androidx.lifecycle.SavedStateHandle
import com.learntogether.util.Categories
import com.learntogether.util.MediaUrlsPartition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

//  Create Post
data class CreatePostUiState(
    val title: String = "",
    val content: String = "",
    /** Comma-separated image, video, and audio URLs (typed in one field). */
    val mediaUrls: String = "",
    val category: String = "",
    val categories: List<String> = Categories.all,
    val isLoading: Boolean = false,
    val isCreated: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateContent(content: String) = _uiState.update { it.copy(content = content) }
    fun updateMediaUrls(urls: String) = _uiState.update { it.copy(mediaUrls = urls) }
    fun updateCategory(category: String) = _uiState.update { it.copy(category = category) }

    fun createPost() {
        val state = _uiState.value
        if (state.title.isBlank() || state.content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title and content are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = userRepository.getCurrentUserOnce()
            if (user != null) {
                val (imgJoined, vidJoined, audJoined) =
                    MediaUrlsPartition.splitToStorageFields(state.mediaUrls)
                postRepository.createPost(
                    authorId = user.userId,
                    title = state.title,
                    content = state.content,
                    imageUrls = MediaUrlsPartition.parseCommaSeparated(imgJoined),
                    videoUrl = vidJoined,
                    audioUrl = audJoined,
                    category = state.category
                )
                _uiState.update { it.copy(isLoading = false, isCreated = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Please log in first") }
            }
        }
    }
}

//  Post Detail
data class PostDetailUiState(
    val post: PostEntity? = null,
    val author: UserEntity? = null,
    val comments: List<CommentEntity> = emptyList(),
    val commentAuthors: Map<String, UserEntity> = emptyMap(),
    val commentLikeCounts: Map<String, Int> = emptyMap(),
    val likedCommentIds: Set<String> = emptySet(),
    val currentUser: UserEntity? = null,
    val isLiked: Boolean = false,
    val commentText: String = "",
    /** When non-null, new comment is a reply to this comment id. */
    val replyingToCommentId: String? = null,
    val replyingToLabel: String? = null,
    val expandedReplyThreads: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val postDeleted: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val commentAuthorJobs = mutableMapOf<String, Job>()
    private var likeCollectJob: Job? = null
    private var authorCollectJob: Job? = null
    private var postLoadJob: Job? = null

    fun loadPost(postId: String) {
        postLoadJob?.cancel()
        authorCollectJob?.cancel()
        commentAuthorJobs.values.forEach { it.cancel() }
        commentAuthorJobs.clear()
        likeCollectJob?.cancel()

        postLoadJob = viewModelScope.launch {
            coroutineScope {
                val scope = this
                launch {
                    userRepository.getCurrentUser().collect { user ->
                        _uiState.update { it.copy(currentUser = user) }
                    }
                }
                launch {
                    postRepository.getPostById(postId).collect { post ->
                        _uiState.update { it.copy(post = post, isLoading = false) }
                        authorCollectJob?.cancel()
                        likeCollectJob?.cancel()
                        if (post != null) {
                            val aid = post.authorId
                            authorCollectJob = scope.launch {
                                userRepository.getUserById(aid).collect { user ->
                                    _uiState.update { it.copy(author = user) }
                                }
                            }
                            startLikeCollector(postId, scope)
                        } else {
                            _uiState.update { it.copy(author = null, isLiked = false) }
                        }
                    }
                }
                launch {
                    combine(
                        postRepository.getComments(postId),
                        postRepository.observeCommentLikeAggregates(postId),
                        userRepository.getCurrentUser().flatMapLatest { user ->
                            val uid = user?.userId
                            if (uid == null) flowOf(emptyList())
                            else postRepository.observeLikedCommentIdsForPost(uid, postId)
                        }
                    ) { comments, aggregates, likedList ->
                        Triple(
                            comments,
                            aggregates.associate { it.commentId to it.likeCount },
                            likedList.toSet()
                        )
                    }.collect { (comments, likeMap, likedSet) ->
                        _uiState.update {
                            it.copy(
                                comments = comments,
                                commentLikeCounts = likeMap,
                                likedCommentIds = likedSet
                            )
                        }
                        val ids = comments.map { it.authorId }.distinct().toSet()
                        commentAuthorJobs.keys.filter { it !in ids }.forEach { id ->
                            commentAuthorJobs.remove(id)?.cancel()
                        }
                        ids.forEach { authorId ->
                            loadCommentAuthor(authorId, scope)
                        }
                    }
                }
            }
        }
    }

    private fun startLikeCollector(postId: String, scope: CoroutineScope) {
        likeCollectJob?.cancel()
        likeCollectJob = scope.launch {
            userRepository.getCurrentUser()
                .flatMapLatest { user ->
                    val uid = user?.userId
                    if (uid == null) flowOf(false)
                    else postRepository.isPostLiked(uid, postId)
                }
                .collect { liked -> _uiState.update { it.copy(isLiked = liked) } }
        }
    }

    private fun loadCommentAuthor(authorId: String, scope: CoroutineScope) {
        if (commentAuthorJobs.containsKey(authorId)) return
        commentAuthorJobs[authorId] = scope.launch {
            userRepository.getUserById(authorId).collect { user ->
                _uiState.update { s ->
                    val m = s.commentAuthors.toMutableMap()
                    if (user != null) m[authorId] = user else m.remove(authorId)
                    s.copy(commentAuthors = m)
                }
            }
        }
    }

    fun updateCommentText(text: String) {
        _uiState.update { it.copy(commentText = text) }
    }

    fun startReply(commentId: String, authorName: String) {
        _uiState.update {
            it.copy(
                replyingToCommentId = commentId,
                replyingToLabel = authorName
            )
        }
    }

    fun clearReply() {
        _uiState.update { it.copy(replyingToCommentId = null, replyingToLabel = null) }
    }

    fun clearCommentComposer() {
        _uiState.update {
            it.copy(commentText = "", replyingToCommentId = null, replyingToLabel = null)
        }
    }

    fun toggleReplyThread(rootCommentId: String) {
        _uiState.update { s ->
            val next = s.expandedReplyThreads.toMutableSet()
            if (!next.add(rootCommentId)) next.remove(rootCommentId)
            s.copy(expandedReplyThreads = next)
        }
    }

    fun toggleLike() {
        val userId = _uiState.value.currentUser?.userId ?: return
        val postId = _uiState.value.post?.postId ?: return
        viewModelScope.launch {
            if (_uiState.value.isLiked) {
                postRepository.unlikePost(userId, postId)
            } else {
                postRepository.likePost(userId, postId)
            }
        }
    }

    fun submitComment() {
        val userId = _uiState.value.currentUser?.userId ?: return
        val postId = _uiState.value.post?.postId ?: return
        val text = _uiState.value.commentText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            val parent = _uiState.value.replyingToCommentId
            postRepository.addComment(postId, userId, text, parentCommentId = parent)
            _uiState.update { it.copy(commentText = "", replyingToCommentId = null, replyingToLabel = null) }
        }
    }

    fun deletePost() {
        val post = _uiState.value.post ?: return
        viewModelScope.launch {
            postRepository.deletePost(post)
            _uiState.update { it.copy(postDeleted = true, post = null) }
        }
    }

    fun toggleCommentLike(commentId: String) {
        val userId = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch { postRepository.toggleCommentLike(userId, commentId) }
    }

    fun deleteComment(commentId: String) {
        val userId = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch {
            val err = postRepository.deleteCommentThread(userId, commentId)
            if (err != null) _uiState.update { it.copy(snackbarMessage = err) }
        }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

//  Edit Post
data class EditPostUiState(
    val title: String = "",
    val content: String = "",
    val mediaUrls: String = "",
    val category: String = "",
    val categories: List<String> = Categories.all,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EditPostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])
    private val _uiState = MutableStateFlow(EditPostUiState())
    val uiState: StateFlow<EditPostUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            postRepository.getPostById(postId).collect { post ->
                if (post != null) {
                    _uiState.update {
                        it.copy(
                            title = post.title,
                            content = post.content,
                            mediaUrls = MediaUrlsPartition.mergeFromStorage(
                                post.imageUrls,
                                post.videoUrl,
                                post.audioUrl
                            ),
                            category = post.category,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Post not found") }
                }
            }
        }
    }

    fun updateTitle(t: String) = _uiState.update { it.copy(title = t) }
    fun updateContent(c: String) = _uiState.update { it.copy(content = c) }
    fun updateMediaUrls(u: String) = _uiState.update { it.copy(mediaUrls = u) }
    fun updateCategory(c: String) = _uiState.update { it.copy(category = c) }

    fun save() {
        val s = _uiState.value
        if (s.title.isBlank() || s.content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title and content are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val existing = postRepository.getPostByIdOnce(postId)
            if (existing == null) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Post not found") }
                return@launch
            }
            val me = userRepository.getCurrentUserOnce()
            if (me?.userId != existing.authorId) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "You can only edit your own posts") }
                return@launch
            }
            val (imgJoined, vidJoined, audJoined) =
                MediaUrlsPartition.splitToStorageFields(s.mediaUrls)
            postRepository.updatePost(
                existing.copy(
                    title = s.title.trim(),
                    content = s.content,
                    imageUrls = imgJoined,
                    videoUrl = vidJoined,
                    audioUrl = audJoined,
                    category = s.category
                )
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
