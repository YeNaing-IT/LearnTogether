package com.learntogether.ui.screens.feed

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.learntogether.ui.components.*
import com.learntogether.util.MediaUrlsPartition
import com.learntogether.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onUserClick: (String) -> Unit,
    onNavigateToPomodoro: () -> Unit,
    onNavigateToChallenges: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Following", "Latest")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LearnTogether",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToPomodoro) {
                        Icon(Icons.Outlined.Timer, contentDescription = "Pomodoro")
                    }
                    IconButton(onClick = onNavigateToChallenges) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = "Challenges")
                    }
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Statistics")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePost,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Create Post")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Quote Card
            if (uiState.dailyQuote.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Daily Inspiration",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "\"${uiState.dailyQuote}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "— ${uiState.quoteAuthor}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Welcome greeting
            item {
                uiState.currentUser?.let { user ->
                    Text(
                        text = "Welcome back, ${user.username.split(" ").first()}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Tab row
            item {
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Posts
            val displayedPosts = if (uiState.selectedTab == 0) uiState.feedPosts else uiState.latestPosts

            if (uiState.isLoading) {
                item { LoadingIndicator(modifier = Modifier.padding(32.dp)) }
            } else if (displayedPosts.isEmpty()) {
                item {
                    if (uiState.selectedTab == 0) {
                        EmptyState(
                            icon = Icons.Outlined.People,
                            title = "No posts from followed users",
                            subtitle = "Follow other learners to see their posts here"
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Article,
                            title = "No posts yet",
                            subtitle = "Tap the pencil button to create a post."
                        )
                    }
                }
            } else {
                items(displayedPosts, key = { it.postId }) { post ->
                    val author = uiState.userCache[post.authorId]
                    val postImages = MediaUrlsPartition.imageUrlsForDisplay(
                        post.imageUrls,
                        post.videoUrl,
                        post.audioUrl
                    )
                    PostCard(
                        authorName = author?.username ?: "Unknown",
                        authorHandle = author?.handle ?: "",
                        authorImageUrl = author?.profilePictureUrl,
                        title = post.title,
                        content = post.content,
                        likeCount = post.likeCount,
                        commentCount = post.commentCount,
                        isLiked = uiState.likedPosts.contains(post.postId),
                        hasImageAttachments = postImages.isNotEmpty(),
                        hasVideoAttachments = MediaUrlsPartition.hasClassifiedVideoUrls(post.videoUrl),
                        hasAudioAttachments = MediaUrlsPartition.hasClassifiedAudioUrls(post.audioUrl),
                        imageUrl = postImages.firstOrNull(),
                        timeAgo = TimeUtils.getTimeAgo(post.createdAt),
                        onLikeClick = { viewModel.toggleLike(post.postId) },
                        onClick = { onPostClick(post.postId) },
                        onAuthorClick = { onUserClick(post.authorId) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
