package com.learntogether.ui.screens.course

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.learntogether.ui.components.TLTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCourseScreen(
    onBack: () -> Unit,
    onCourseCreated: () -> Unit,
    viewModel: CreateCourseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategoryMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isCreated) { if (uiState.isCreated) onCourseCreated() }

    Scaffold(
        topBar = {
            TLTopBar(title = "Create Course", onBack = onBack, actions = {
                TextButton(
                    onClick = { viewModel.createCourse() },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Create", fontWeight = FontWeight.Bold)
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Course Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = uiState.title, onValueChange = viewModel::updateTitle,
                label = { Text("Course Title") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            OutlinedTextField(
                value = uiState.description, onValueChange = viewModel::updateDescription,
                label = { Text("Description") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp), maxLines = 5
            )

            ExposedDropdownMenuBox(expanded = showCategoryMenu, onExpandedChange = { showCategoryMenu = it }) {
                OutlinedTextField(
                    value = uiState.category.ifBlank { "Select Category" }, onValueChange = {},
                    readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp),
                    label = { Text("Category") }
                )
                ExposedDropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                    uiState.categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { viewModel.updateCategory(cat); showCategoryMenu = false })
                    }
                }
            }

            OutlinedTextField(
                value = uiState.durationDays, onValueChange = viewModel::updateDurationDays,
                label = { Text("Duration (days, 0 = no deadline)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )

            OutlinedTextField(
                value = uiState.imageUrl, onValueChange = viewModel::updateImageUrl,
                label = { Text("Cover Image URL (optional)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Private course", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Only people with the access code can enroll",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isPrivate,
                    onCheckedChange = viewModel::updateIsPrivate
                )
            }
            if (uiState.isPrivate) {
                OutlinedTextField(
                    value = uiState.accessCode,
                    onValueChange = viewModel::updateAccessCode,
                    label = { Text("Access code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Chapters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.addChapter() }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Chapter")
                }
            }

            uiState.chapters.forEachIndexed { index, chapter ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Chapter ${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            if (uiState.chapters.size > 1) {
                                IconButton(onClick = { viewModel.removeChapter(index) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        OutlinedTextField(
                            value = chapter.title,
                            onValueChange = { viewModel.updateChapter(index, chapter.copy(title = it)) },
                            label = { Text("Chapter Title") }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp), singleLine = true
                        )
                        OutlinedTextField(
                            value = chapter.content,
                            onValueChange = { viewModel.updateChapter(index, chapter.copy(content = it)) },
                            label = { Text("Chapter Content") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                            shape = RoundedCornerShape(8.dp), maxLines = 10
                        )
                        OutlinedTextField(
                            value = chapter.mediaUrls,
                            onValueChange = { viewModel.updateChapter(index, chapter.copy(mediaUrls = it)) },
                            label = { Text("Media URLs (optional)") },
                            placeholder = { Text("Comma-separated links: images, video, or audio") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 3,
                            maxLines = 8
                        )
                        OutlinedTextField(
                            value = chapter.durationMinutes,
                            onValueChange = { viewModel.updateChapter(index, chapter.copy(durationMinutes = it)) },
                            label = { Text("Minutes") },
                            modifier = Modifier.width(120.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }
            }

            AnimatedVisibility(visible = uiState.error != null) {
                Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
