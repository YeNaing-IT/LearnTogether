package com.learntogether.ui.screens.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.learntogether.ui.components.*
import com.learntogether.util.MediaUrlsPartition
import com.learntogether.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    onCourseClick: (String) -> Unit,
    onCreateCourse: () -> Unit,
    viewModel: CoursesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("All Courses", "My Ongoing")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Courses", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateCourse,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Course")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(title, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            when (uiState.selectedTab) {
                0 -> {
                    if (uiState.isLoading) {
                        item { LoadingIndicator(modifier = Modifier.padding(32.dp)) }
                    } else if (uiState.courses.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.MenuBook,
                                title = "No courses yet",
                                subtitle = "Create the first course from the + button."
                            )
                        }
                    } else {
                        items(uiState.courses, key = { it.courseId }) { course ->
                            val creator = uiState.creatorCache[course.creatorId]
                            CourseCard(
                                title = course.title,
                                description = course.description,
                                category = course.category,
                                creatorName = creator?.username ?: "Unknown",
                                enrollmentCount = course.enrollmentCount,
                                durationDays = course.durationDays,
                                imageUrl = MediaUrlsPartition.firstCommaSeparatedUrl(course.imageUrl),
                                isPrivate = course.isPrivate,
                                totalChapters = uiState.chapterCountByCourseId[course.courseId],
                                onClick = { onCourseClick(course.courseId) }
                            )
                        }
                    }
                }
                1 -> {
                    if (uiState.ongoingEnrollments.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Outlined.School,
                                title = "No ongoing courses",
                                subtitle = "Enroll in a course to start learning"
                            )
                        }
                    } else {
                        items(uiState.ongoingEnrollments, key = { "${it.userId}_${it.courseId}" }) { enrollment ->
                            val course = uiState.courses.find { it.courseId == enrollment.courseId }
                            if (course != null) {
                                val creator = uiState.creatorCache[course.creatorId]
                                val completedCount = enrollment.completedChapters.split(",").filter { it.isNotBlank() }.size
                                val totalChapters = uiState.chapterCountByCourseId[course.courseId] ?: 0
                                val deadlinePair = enrollment.deadlineAt?.let { deadline ->
                                    val days = TimeUtils.getDaysRemaining(deadline)
                                    "$days days left" to (days < 7)
                                }
                                OngoingCourseEnrollmentCard(
                                    title = course.title,
                                    description = course.description,
                                    category = course.category,
                                    creatorName = creator?.username ?: "Unknown",
                                    enrollmentCount = course.enrollmentCount,
                                    durationDays = course.durationDays,
                                    imageUrl = MediaUrlsPartition.firstCommaSeparatedUrl(course.imageUrl),
                                    isPrivate = course.isPrivate,
                                    completedChapters = completedCount,
                                    totalChapters = totalChapters,
                                    deadlineSummary = deadlinePair?.first,
                                    deadlineUrgent = deadlinePair?.second ?: false,
                                    onClick = { onCourseClick(course.courseId) }
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
