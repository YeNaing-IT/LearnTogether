package com.learntogether.ui.screens.post

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.learntogether.data.local.entity.CommentEntity
import com.learntogether.data.local.entity.UserEntity
import com.learntogether.ui.components.*
import com.learntogether.util.MediaUrlsPartition
import com.learntogether.util.TimeUtils
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onEditPost: (String) -> Unit = {},
    onPostDeleted: () -> Unit = {},
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var composerVisible by remember(postId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) { viewModel.loadPost(postId) }

    LaunchedEffect(uiState.postDeleted) {
        if (uiState.postDeleted) onPostDeleted()
    }

    val postOrNull = uiState.post
    val isOwner = postOrNull != null && uiState.currentUser?.userId == postOrNull.authorId
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val m = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m)
        viewModel.consumeSnackbar()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TLTopBar(
                title = "Post",
                onBack = onBack,
                actions = {
                    if (isOwner) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        menuOpen = false
                                        onEditPost(postId)
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        menuOpen = false
                                        deleteOpen = true
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            val showComposer =
                uiState.currentUser != null &&
                    (composerVisible || uiState.replyingToCommentId != null)
            if (showComposer) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        if (uiState.replyingToLabel != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Replying to ${uiState.replyingToLabel}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { viewModel.clearReply() }) { Text("Cancel") }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    composerVisible = false
                                    viewModel.clearCommentComposer()
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Close comment box"
                                )
                            }
                            OutlinedTextField(
                                value = uiState.commentText,
                                onValueChange = viewModel::updateCommentText,
                                placeholder = {
                                    Text(if (uiState.replyingToCommentId != null) "Write a reply…" else "Add a comment…")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.submitComment() },
                                enabled = uiState.commentText.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (uiState.commentText.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (deleteOpen) {
            AlertDialog(
                onDismissRequest = { deleteOpen = false },
                title = { Text("Delete post?") },
                text = { Text("This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteOpen = false
                            viewModel.deletePost()
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { deleteOpen = false }) { Text("Cancel") }
                }
            )
        }
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.post == null) {
            EmptyState(
                icon = Icons.Outlined.Article,
                title = "Post not found",
                subtitle = "This post may have been deleted"
            )
        } else {
            val post = uiState.post!!
            val author = uiState.author

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Author info
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UserAvatar(
                            imageUrl = author?.profilePictureUrl,
                            username = author?.username ?: "?",
                            size = 48,
                            onClick = { author?.userId?.let(onUserClick) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = author?.username ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "@${author?.handle ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = TimeUtils.getTimeAgo(post.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Title & category
                item {
                    Column {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (post.category.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text(post.category) },
                                icon = {
                                    Icon(
                                        Icons.Outlined.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Media (image column + legacy image-like URLs stored under video/audio)
                val images = MediaUrlsPartition.imageUrlsForDisplay(
                    post.imageUrls,
                    post.videoUrl,
                    post.audioUrl
                )
                if (images.isNotEmpty()) {
                    items(images.size, key = { idx -> "${images[idx]}_$idx" }) { idx ->
                        AsyncImage(
                            model = images[idx],
                            contentDescription = "Post image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }

                // Content body
                item {
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Media indicators (only when stored fields contain real video/audio, not misclassified images)
                val showVideoChip = MediaUrlsPartition.hasClassifiedVideoUrls(post.videoUrl)
                val showAudioChip = MediaUrlsPartition.hasClassifiedAudioUrls(post.audioUrl)
                if (showVideoChip || showAudioChip) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (showVideoChip) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Video") },
                                    leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                            if (showAudioChip) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Audio") },
                                    leadingIcon = { Icon(Icons.Outlined.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }

                // Like / Comment actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.toggleLike() },
                            label = { Text("${post.likeCount} Likes") },
                            leadingIcon = {
                                Icon(
                                    if (uiState.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (uiState.isLiked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        AssistChip(
                            onClick = {
                                if (uiState.currentUser == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Sign in to comment")
                                    }
                                } else if (composerVisible) {
                                    composerVisible = false
                                    viewModel.clearCommentComposer()
                                } else {
                                    composerVisible = true
                                }
                            },
                            label = { Text("${post.commentCount} Comments") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                // Divider
                item { HorizontalDivider() }

                // Comments header
                item {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comments (threaded; extra direct replies collapsed like Facebook)
                if (uiState.comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Be the first to share your thoughts!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val childrenByParent = uiState.comments
                        .groupBy { it.parentCommentId }
                        .mapValues { (_, list) -> list.sortedBy { it.createdAt } }
                    val roots = childrenByParent[null].orEmpty()
                    items(roots, key = { it.commentId }) { root ->
                        CommentThreadBlock(
                            comment = root,
                            depth = 0,
                            childrenByParent = childrenByParent,
                            commentAuthors = uiState.commentAuthors,
                            expandedParentIds = uiState.expandedReplyThreads,
                            onToggleReplies = viewModel::toggleReplyThread,
                            onReply = { id, name ->
                                if (uiState.currentUser != null) {
                                    composerVisible = true
                                    viewModel.startReply(id, name)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Sign in to comment")
                                    }
                                }
                            },
                            onUserClick = onUserClick,
                            currentUserId = uiState.currentUser?.userId,
                            postAuthorId = uiState.post?.authorId,
                            commentLikeCounts = uiState.commentLikeCounts,
                            likedCommentIds = uiState.likedCommentIds,
                            onToggleCommentLike = viewModel::toggleCommentLike,
                            onDeleteComment = viewModel::deleteComment
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CommentThreadBlock(
    comment: CommentEntity,
    depth: Int,
    childrenByParent: Map<String?, List<CommentEntity>>,
    commentAuthors: Map<String, UserEntity>,
    expandedParentIds: Set<String>,
    onToggleReplies: (String) -> Unit,
    onReply: (String, String) -> Unit,
    onUserClick: (String) -> Unit,
    currentUserId: String?,
    postAuthorId: String?,
    commentLikeCounts: Map<String, Int>,
    likedCommentIds: Set<String>,
    onToggleCommentLike: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val commentAuthor = commentAuthors[comment.authorId]
    val displayName = commentAuthor?.username ?: "Unknown"
    val indent = if (depth == 0) 0.dp else 12.dp + (depth * 8).dp
    val likeCount = commentLikeCounts[comment.commentId] ?: 0
    val liked = comment.commentId in likedCommentIds
    val canDelete = currentUserId != null &&
        (comment.authorId == currentUserId || postAuthorId == currentUserId)
    var commentMenuOpen by remember(comment.commentId) { mutableStateOf(false) }
    var confirmDelete by remember(comment.commentId) { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete comment?") },
            text = { Text("Replies will be removed too.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteComment(comment.commentId)
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            UserAvatar(
                imageUrl = commentAuthor?.profilePictureUrl,
                username = displayName,
                size = if (depth == 0) 36 else 32,
                onClick = { commentAuthor?.userId?.let(onUserClick) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = TimeUtils.getTimeAgo(comment.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canDelete) {
                        Box {
                            IconButton(
                                onClick = { commentMenuOpen = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Comment options")
                            }
                            DropdownMenu(
                                expanded = commentMenuOpen,
                                onDismissRequest = { commentMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        commentMenuOpen = false
                                        confirmDelete = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentUserId != null) {
                        IconButton(
                            onClick = { onToggleCommentLike(comment.commentId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (liked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (likeCount > 0) {
                            Text(
                                text = "$likeCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onReply(comment.commentId, displayName) }
                    )
                }
            }
        }

        val children = childrenByParent[comment.commentId].orEmpty()
        if (children.isNotEmpty()) {
            val expanded = expandedParentIds.contains(comment.commentId)
            val visible = if (children.size <= 1 || expanded) children else listOf(children.first())
            Column(Modifier.padding(start = 8.dp)) {
                visible.forEach { child ->
                    CommentThreadBlock(
                        comment = child,
                        depth = depth + 1,
                        childrenByParent = childrenByParent,
                        commentAuthors = commentAuthors,
                        expandedParentIds = expandedParentIds,
                        onToggleReplies = onToggleReplies,
                        onReply = onReply,
                        onUserClick = onUserClick,
                        currentUserId = currentUserId,
                        postAuthorId = postAuthorId,
                        commentLikeCounts = commentLikeCounts,
                        likedCommentIds = likedCommentIds,
                        onToggleCommentLike = onToggleCommentLike,
                        onDeleteComment = onDeleteComment
                    )
                }
                if (children.size > 1) {
                    TextButton(onClick = { onToggleReplies(comment.commentId) }) {
                        Text(
                            if (expanded) "Hide replies"
                            else "View ${children.size - 1} more ${if (children.size == 2) "reply" else "replies"}"
                        )
                    }
                }
            }
        }
    }
}
