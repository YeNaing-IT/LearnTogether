package com.learntogether.ui.screens.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.learntogether.ui.components.*
import com.learntogether.util.MediaUrlsPartition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    chapterId: String,
    onBack: () -> Unit,
    onChatClick: () -> Unit,
    onEditChapter: (String) -> Unit = {},
    viewModel: ChapterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(chapterId) { viewModel.load(chapterId) }

    Scaffold(
        topBar = {
            TLTopBar(
                title = uiState.chapter?.title ?: "Chapter",
                onBack = onBack,
                actions = {
                    if (uiState.isCourseOwner && uiState.chapter != null) {
                        IconButton(onClick = { onEditChapter(uiState.chapter!!.chapterId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit chapter")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!uiState.isLoading && uiState.chapter != null && uiState.isEnrolled) {
                Surface(tonalElevation = 3.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.isChapterCompleted) {
                            OutlinedButton(
                                onClick = { viewModel.toggleChapterCompletion() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Completed — tap to undo")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.toggleChapterCompletion() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Mark as completed")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else if (uiState.chapter == null) {
            EmptyState(icon = Icons.Outlined.Article, title = "Chapter not found", subtitle = "")
        } else {
            val chapter = uiState.chapter!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (chapter.durationMinutes > 0) {
                    Row {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${chapter.durationMinutes} minutes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Media indicators (video/audio only; images are shown inline below)
                if (MediaUrlsPartition.hasClassifiedVideoUrls(chapter.videoUrl)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Video Available") },
                        leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                if (MediaUrlsPartition.hasClassifiedAudioUrls(chapter.audioUrl)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Audio Available") },
                        leadingIcon = { Icon(Icons.Outlined.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                val chapterImages = MediaUrlsPartition.imageUrlsForDisplay(
                    chapter.imageUrl,
                    chapter.videoUrl,
                    chapter.audioUrl
                )
                if (chapterImages.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        chapterImages.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Chapter image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Content
                Text(
                    text = chapter.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Discussion button
                OutlinedButton(onClick = onChatClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Join Chapter Discussion")
                }
            }
        }
    }
}
