package com.learntogether.ui.screens.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.learntogether.ui.components.*
import com.learntogether.util.MediaUrlsPartition
import com.learntogether.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    onBack: () -> Unit,
    onChapterClick: (String) -> Unit,
    onChapterChatClick: (String) -> Unit,
    onEditCourse: (String) -> Unit = {},
    onCourseDeleted: () -> Unit = {},
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var unenrollConfirmOpen by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var newChapterTitle by remember { mutableStateOf("") }
    var newChapterContent by remember { mutableStateOf("") }
    var newAnnouncementText by remember { mutableStateOf("") }
    var chapterPendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    LaunchedEffect(uiState.courseDeleted) {
        if (uiState.courseDeleted) onCourseDeleted()
    }

    val isCreator = uiState.course != null && uiState.currentUser?.userId == uiState.course!!.creatorId

    Scaffold(
        topBar = {
            TLTopBar(
                title = "Course",
                onBack = onBack,
                actions = {
                    if (isCreator) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit course") },
                                    onClick = {
                                        menuOpen = false
                                        onEditCourse(courseId)
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete course") },
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
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding).padding(32.dp))
        } else if (uiState.course == null) {
            EmptyState(icon = Icons.Outlined.MenuBook, title = "Course not found", subtitle = "This course may have been deleted")
        } else {
            val course = uiState.course!!
            val creator = uiState.creator
            val totalChapters = uiState.chapters.size
            val completedCount = uiState.completedChapterIds.size

            if (showAddChapterDialog) {
                AlertDialog(
                    onDismissRequest = { showAddChapterDialog = false },
                    title = { Text("Add chapter") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newChapterTitle,
                                onValueChange = { newChapterTitle = it },
                                label = { Text("Chapter title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newChapterContent,
                                onValueChange = { newChapterContent = it },
                                label = { Text("Content (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newChapterTitle.isNotBlank()) {
                                    viewModel.addChapterAsOwner(newChapterTitle, newChapterContent)
                                    newChapterTitle = ""
                                    newChapterContent = ""
                                    showAddChapterDialog = false
                                }
                            }
                        ) { Text("Add") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddChapterDialog = false }) { Text("Cancel") }
                    }
                )
            }

            chapterPendingDeleteId?.let { deleteChapterId ->
                val chapterTitle = uiState.chapters.find { it.chapterId == deleteChapterId }?.title ?: "this chapter"
                AlertDialog(
                    onDismissRequest = { chapterPendingDeleteId = null },
                    title = { Text("Delete chapter?") },
                    text = { Text("“$chapterTitle” will be removed. Learner progress for this chapter will be cleared. This cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteChapterAsOwner(deleteChapterId)
                                chapterPendingDeleteId = null
                            }
                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { chapterPendingDeleteId = null }) { Text("Cancel") }
                    }
                )
            }

            if (showAnnouncementDialog) {
                AlertDialog(
                    onDismissRequest = { showAnnouncementDialog = false },
                    title = { Text("New announcement") },
                    text = {
                        OutlinedTextField(
                            value = newAnnouncementText,
                            onValueChange = { newAnnouncementText = it },
                            label = { Text("Message to learners") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newAnnouncementText.isNotBlank()) {
                                    viewModel.postAnnouncementAsOwner(newAnnouncementText)
                                    newAnnouncementText = ""
                                    showAnnouncementDialog = false
                                }
                            }
                        ) { Text("Post") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAnnouncementDialog = false }) { Text("Cancel") }
                    }
                )
            }

            if (deleteOpen) {
                AlertDialog(
                    onDismissRequest = { deleteOpen = false },
                    title = { Text("Delete course?") },
                    text = { Text("All chapters, enrollments, and chat for this course will be removed.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                deleteOpen = false
                                viewModel.deleteCourse()
                            }
                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteOpen = false }) { Text("Cancel") }
                    }
                )
            }

            if (unenrollConfirmOpen) {
                AlertDialog(
                    onDismissRequest = {
                        unenrollConfirmOpen = false
                        viewModel.clearUnenrollError()
                    },
                    title = { Text("Leave this course?") },
                    text = { Text("You will lose access to chapters until you enroll again. Your progress for this enrollment will be removed.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                unenrollConfirmOpen = false
                                viewModel.unenroll()
                            }
                        ) { Text("Leave course", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            unenrollConfirmOpen = false
                            viewModel.clearUnenrollError()
                        }) { Text("Cancel") }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            ))),
                        contentAlignment = Alignment.Center
                    ) {
                        val coverUrl = MediaUrlsPartition.firstCommaSeparatedUrl(course.imageUrl)
                        if (!coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Outlined.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Title & info
                item {
                    Column {
                        Text(course.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (course.isPrivate) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Private — access code required to enroll") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(course.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (course.category.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            SuggestionChip(onClick = {}, label = { Text(course.category) })
                        }
                    }
                }

                // Creator & stats
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(imageUrl = creator?.profilePictureUrl, username = creator?.username ?: "?", size = 32)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(creator?.username ?: "Unknown", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                                Text("Creator", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${course.enrollmentCount} enrolled", style = MaterialTheme.typography.labelMedium)
                            if (course.durationDays > 0) Text("${course.durationDays} days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Progress (if enrolled)
                if (uiState.isEnrolled) {
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("$completedCount / $totalChapters chapters", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalChapters > 0) completedCount.toFloat() / totalChapters else 0f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                uiState.enrollment?.deadlineAt?.let { deadline ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val days = TimeUtils.getDaysRemaining(deadline)
                                    Text("Deadline: ${TimeUtils.formatDate(deadline)} ($days days remaining)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (days < 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Leave course (enrolled learners, including creator if they enrolled)
                if (uiState.isEnrolled) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { unenrollConfirmOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Leave course")
                            }
                            uiState.unenrollError?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Enroll (public) or access code + enroll (private, not creator); creator actions sit above Enroll
                if (!uiState.isEnrolled) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isCreator) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showAddChapterDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add chapter")
                                    }
                                    OutlinedButton(
                                        onClick = { showAnnouncementDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Announcement")
                                    }
                                }
                                uiState.ownerActionError?.let { err ->
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (course.isPrivate && !isCreator) {
                                OutlinedTextField(
                                    value = uiState.accessCodeInput,
                                    onValueChange = viewModel::updateAccessCodeInput,
                                    label = { Text("Access code") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                            Button(
                                onClick = { viewModel.enroll() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enroll in Course", fontWeight = FontWeight.SemiBold)
                            }
                            uiState.enrollError?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Creator enrolled: same actions (no Enroll row)
                if (isCreator && uiState.isEnrolled) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAddChapterDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add chapter")
                                }
                                OutlinedButton(
                                    onClick = { showAnnouncementDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Announcement")
                                }
                            }
                            uiState.ownerActionError?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Announcements (after course exists; visible to everyone viewing the course)
                if (uiState.announcements.isNotEmpty()) {
                    item {
                        Text("Announcements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(uiState.announcements, key = { it.announcementId }) { ann ->
                        val author = uiState.announcementAuthors[ann.authorId]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        author?.username ?: "Instructor",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ann.message, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        TimeUtils.getTimeAgo(ann.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                if (isCreator) {
                                    IconButton(onClick = { viewModel.deleteAnnouncementAsOwner(ann.announcementId) }) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Delete announcement",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chapters (tight vertical spacing; each block is tall with content preview)
                item {
                    Text("Chapters (${uiState.chapters.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.chapters.forEachIndexed { index, chapter ->
                            val isCompleted = uiState.completedChapterIds.contains(chapter.chapterId)
                            val canOpenChapter = isCreator || uiState.isEnrolled
                            val previewText = chapter.content.trim().replace("\n", " ").takeIf { it.isNotBlank() }
                                ?: "No content yet."
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (canOpenChapter) 1f else 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompleted) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    }
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = canOpenChapter) { onChapterClick(chapter.chapterId) }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCompleted) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surface
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCompleted) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(22.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                } else {
                                                    Text(
                                                        "${index + 1}",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    chapter.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (chapter.durationMinutes > 0) {
                                                    Text(
                                                        "${chapter.durationMinutes} min",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    previewText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 5,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (!canOpenChapter) {
                                                    Text(
                                                        "Enroll to access",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(top = 6.dp)
                                                    )
                                                }
                                            }
                                            if (!canOpenChapter) {
                                                Icon(
                                                    Icons.Outlined.Lock,
                                                    contentDescription = "Locked",
                                                    modifier = Modifier
                                                        .padding(top = 4.dp)
                                                        .size(22.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row {
                                            if (uiState.isEnrolled) {
                                                IconButton(
                                                    onClick = { viewModel.toggleChapterCompletion(chapter.chapterId) }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                                        contentDescription = if (isCompleted) {
                                                            "Mark chapter as not completed"
                                                        } else {
                                                            "Mark chapter as completed"
                                                        },
                                                        modifier = Modifier.size(22.dp),
                                                        tint = if (isCompleted) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Row {
                                            if (canOpenChapter) {
                                                IconButton(onClick = { onChapterChatClick(chapter.chapterId) }) {
                                                    Icon(
                                                        Icons.Outlined.Forum,
                                                        contentDescription = "Discussion",
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            if (isCreator) {
                                                IconButton(onClick = { chapterPendingDeleteId = chapter.chapterId }) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Delete chapter",
                                                        tint = MaterialTheme.colorScheme.error
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

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
