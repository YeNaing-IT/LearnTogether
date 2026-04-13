package com.learntogether.ui.screens.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.*
import com.learntogether.data.repository.CourseRepository
import com.learntogether.data.repository.UserRepository
import com.learntogether.util.Categories
import com.learntogether.util.MediaUrlsPartition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

//  Courses List
data class CoursesUiState(
    val courses: List<CourseEntity> = emptyList(),
    val enrolledCourseIds: Set<String> = emptySet(),
    val ongoingEnrollments: List<EnrollmentEntity> = emptyList(),
    val chapterCountByCourseId: Map<String, Int> = emptyMap(),
    val creatorCache: Map<String, UserEntity> = emptyMap(),
    val currentUser: UserEntity? = null,
    val selectedTab: Int = 0,
    /** Topic filter for the Topics tab; null = all topics. */
    val selectedTopic: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoursesUiState())
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    private val creatorObserveJobs = mutableMapOf<String, Job>()
    private var ongoingEnrollmentsJob: Job? = null

    init {
        viewModelScope.launch {
            coroutineScope {
                userRepository.getCurrentUser().collect { user ->
                    _uiState.update { it.copy(currentUser = user) }
                    ongoingEnrollmentsJob?.cancel()
                    if (user != null) {
                        val uid = user.userId
                        ongoingEnrollmentsJob = launch {
                            courseRepository.getOngoingEnrollments(uid).collect { enrollments ->
                                _uiState.update {
                                    it.copy(
                                        ongoingEnrollments = enrollments,
                                        enrolledCourseIds = enrollments.map { e -> e.courseId }.toSet()
                                    )
                                }
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                ongoingEnrollments = emptyList(),
                                enrolledCourseIds = emptySet()
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(
                courseRepository.getAllCourses(),
                courseRepository.observeChapterCountsByCourse()
            ) { courses, countRows ->
                val countMap = countRows.associate { it.courseId to it.chapterCount }
                val chapterCounts = courses.associate { c ->
                    c.courseId to (countMap[c.courseId] ?: 0)
                }
                courses to chapterCounts
            }.collect { (courses, chapterCounts) ->
                _uiState.update {
                    it.copy(courses = courses, chapterCountByCourseId = chapterCounts, isLoading = false)
                }
                courses.map { it.creatorId }.distinct().forEach { id -> loadCreator(id) }
            }
        }
    }

    private fun loadCreator(userId: String) {
        if (creatorObserveJobs.containsKey(userId)) return
        creatorObserveJobs[userId] = viewModelScope.launch {
            userRepository.getUserById(userId).collect { user ->
                _uiState.update { state ->
                    val next = if (user != null) state.creatorCache + (userId to user) else state.creatorCache - userId
                    state.copy(creatorCache = next)
                }
            }
        }
    }

    fun selectTab(index: Int) { _uiState.update { it.copy(selectedTab = index) } }

    fun selectTopic(topic: String?) {
        _uiState.update {
            it.copy(selectedTopic = if (it.selectedTopic == topic) null else topic)
        }
    }
}

//  Create Course
data class CreateCourseUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val durationDays: String = "",
    val imageUrl: String = "",
    val isPrivate: Boolean = false,
    val accessCode: String = "",
    val chapters: List<ChapterInput> = listOf(ChapterInput()),
    val categories: List<String> = Categories.all,
    val isLoading: Boolean = false,
    val isCreated: Boolean = false,
    val error: String? = null
)

data class ChapterInput(
    val title: String = "",
    val content: String = "",
    /** Comma-separated image, video, and audio URLs in one field. */
    val mediaUrls: String = "",
    val durationMinutes: String = ""
)

@HiltViewModel
class CreateCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCourseUiState())
    val uiState: StateFlow<CreateCourseUiState> = _uiState.asStateFlow()

    fun updateTitle(v: String) = _uiState.update { it.copy(title = v) }
    fun updateDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun updateCategory(v: String) = _uiState.update { it.copy(category = v) }
    fun updateDurationDays(v: String) = _uiState.update { it.copy(durationDays = v) }
    fun updateImageUrl(v: String) = _uiState.update { it.copy(imageUrl = v) }
    fun updateIsPrivate(v: Boolean) = _uiState.update { it.copy(isPrivate = v) }
    fun updateAccessCode(v: String) = _uiState.update { it.copy(accessCode = v) }

    fun updateChapter(index: Int, chapter: ChapterInput) {
        _uiState.update {
            val list = it.chapters.toMutableList()
            if (index < list.size) list[index] = chapter
            it.copy(chapters = list)
        }
    }

    fun addChapter() {
        _uiState.update { it.copy(chapters = it.chapters + ChapterInput()) }
    }

    fun removeChapter(index: Int) {
        _uiState.update {
            if (it.chapters.size > 1) it.copy(chapters = it.chapters.toMutableList().apply { removeAt(index) })
            else it
        }
    }

    fun createCourse() {
        val s = _uiState.value
        if (s.title.isBlank() || s.description.isBlank()) {
            _uiState.update { it.copy(error = "Title and description are required") }; return
        }
        if (s.chapters.any { it.title.isBlank() }) {
            _uiState.update { it.copy(error = "All chapters need a title") }; return
        }
        if (s.isPrivate && s.accessCode.isBlank()) {
            _uiState.update { it.copy(error = "Private courses need an access code") }; return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = userRepository.getCurrentUserOnce()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Please log in") }; return@launch
            }
            val course = courseRepository.createCourse(
                creatorId = user.userId,
                title = s.title,
                description = s.description,
                category = s.category,
                imageUrl = s.imageUrl,
                durationDays = s.durationDays.toIntOrNull() ?: 0,
                isPrivate = s.isPrivate,
                accessCode = s.accessCode
            )
            s.chapters.forEachIndexed { i, ch ->
                val (imgJoined, vidJoined, audJoined) =
                    MediaUrlsPartition.splitToStorageFields(ch.mediaUrls)
                courseRepository.addChapter(
                    courseId = course.courseId,
                    title = ch.title,
                    content = ch.content,
                    orderIndex = i,
                    videoUrl = vidJoined,
                    audioUrl = audJoined,
                    imageUrl = imgJoined,
                    durationMinutes = ch.durationMinutes.toIntOrNull() ?: 0
                )
            }
            _uiState.update { it.copy(isLoading = false, isCreated = true) }
        }
    }
}

//  Course Detail
data class CourseDetailUiState(
    val course: CourseEntity? = null,
    val creator: UserEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val announcements: List<CourseAnnouncementEntity> = emptyList(),
    val announcementAuthors: Map<String, UserEntity> = emptyMap(),
    val enrollment: EnrollmentEntity? = null,
    val currentUser: UserEntity? = null,
    val isEnrolled: Boolean = false,
    val completedChapterIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val courseDeleted: Boolean = false,
    val enrollError: String? = null,
    val accessCodeInput: String = "",
    val unenrollError: String? = null,
    /** Owner-only actions: add chapter, announcement, visibility. */
    val ownerActionError: String? = null
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private var courseDetailLoadJob: Job? = null
    private var enrollmentFlowJob: Job? = null
    private var creatorFlowJob: Job? = null
    private val announcementAuthorJobs = mutableMapOf<String, Job>()

    fun load(courseId: String) {
        courseDetailLoadJob?.cancel()
        enrollmentFlowJob?.cancel()
        creatorFlowJob?.cancel()
        courseDetailLoadJob = viewModelScope.launch {
            coroutineScope {
                launch {
                    userRepository.getCurrentUser().collect { user ->
                        _uiState.update { it.copy(currentUser = user) }
                        enrollmentFlowJob?.cancel()
                        if (user != null) {
                            val uid = user.userId
                            enrollmentFlowJob = launch {
                                courseRepository.getEnrollmentFlow(uid, courseId).collect { enrollment ->
                                    val completedIds = enrollment?.completedChapters
                                        ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
                                        ?: emptySet()
                                    _uiState.update {
                                        it.copy(
                                            enrollment = enrollment,
                                            isEnrolled = enrollment != null,
                                            completedChapterIds = completedIds
                                        )
                                    }
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    enrollment = null,
                                    isEnrolled = false,
                                    completedChapterIds = emptySet()
                                )
                            }
                        }
                    }
                }
                launch {
                    courseRepository.getCourseById(courseId).collect { course ->
                        _uiState.update {
                            it.copy(course = course, isLoading = false, enrollError = null)
                        }
                        creatorFlowJob?.cancel()
                        if (course != null) {
                            val crId = course.creatorId
                            creatorFlowJob = launch {
                                userRepository.getUserById(crId).collect { creator ->
                                    _uiState.update { it.copy(creator = creator) }
                                }
                            }
                        } else {
                            _uiState.update { it.copy(creator = null) }
                        }
                    }
                }
                launch {
                    courseRepository.getChaptersByCourse(courseId).collect { chapters ->
                        _uiState.update { it.copy(chapters = chapters) }
                    }
                }
                launch {
                    courseRepository.getAnnouncementsForCourse(courseId).collect { list ->
                        _uiState.update { it.copy(announcements = list) }
                        list.map { it.authorId }.distinct().forEach { loadAnnouncementAuthor(it) }
                    }
                }
            }
        }
    }

    private fun loadAnnouncementAuthor(userId: String) {
        if (announcementAuthorJobs.containsKey(userId)) return
        announcementAuthorJobs[userId] = viewModelScope.launch {
            userRepository.getUserById(userId).collect { user ->
                _uiState.update { s ->
                    val next = if (user != null) s.announcementAuthors + (userId to user) else s.announcementAuthors - userId
                    s.copy(announcementAuthors = next)
                }
            }
        }
    }

    fun clearOwnerActionError() {
        _uiState.update { it.copy(ownerActionError = null) }
    }

    fun addChapterAsOwner(title: String, content: String) {
        val course = _uiState.value.course ?: return
        val me = _uiState.value.currentUser ?: return
        if (course.creatorId != me.userId) return
        if (title.isBlank()) {
            _uiState.update { it.copy(ownerActionError = "Chapter title is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(ownerActionError = null) }
            val order = (_uiState.value.chapters.maxOfOrNull { it.orderIndex } ?: -1) + 1
            courseRepository.addChapter(course.courseId, title.trim(), content.trim(), order)
        }
    }

    fun postAnnouncementAsOwner(message: String) {
        val course = _uiState.value.course ?: return
        val me = _uiState.value.currentUser ?: return
        if (course.creatorId != me.userId) return
        if (message.isBlank()) {
            _uiState.update { it.copy(ownerActionError = "Announcement cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(ownerActionError = null) }
            courseRepository.addCourseAnnouncement(course.courseId, me.userId, message.trim())
        }
    }

    fun deleteAnnouncementAsOwner(announcementId: String) {
        val me = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch {
            val err = courseRepository.deleteCourseAnnouncement(announcementId, me)
            _uiState.update { it.copy(ownerActionError = err) }
        }
    }

    fun updateAccessCodeInput(v: String) = _uiState.update { it.copy(accessCodeInput = v, enrollError = null) }

    fun enroll() {
        val userId = _uiState.value.currentUser?.userId ?: return
        val course = _uiState.value.course ?: return
        val courseId = course.courseId
        viewModelScope.launch {
            _uiState.update { it.copy(enrollError = null) }
            val code = when {
                course.creatorId == userId -> ""
                course.isPrivate -> _uiState.value.accessCodeInput
                else -> ""
            }
            val err = courseRepository.enrollInCourseWithCode(userId, courseId, code)
            if (err != null) {
                _uiState.update { it.copy(enrollError = err) }
            } else if (course.creatorId != userId) {
                _uiState.update { it.copy(accessCodeInput = "") }
            }
        }
    }

    fun unenroll() {
        val userId = _uiState.value.currentUser?.userId ?: return
        val courseId = _uiState.value.course?.courseId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(unenrollError = null) }
            val err = courseRepository.unenrollFromCourse(userId, courseId)
            if (err != null) _uiState.update { it.copy(unenrollError = err) }
        }
    }

    fun clearUnenrollError() {
        _uiState.update { it.copy(unenrollError = null) }
    }

    fun toggleChapterCompletion(chapterId: String) {
        val userId = _uiState.value.currentUser?.userId ?: return
        val courseId = _uiState.value.course?.courseId ?: return
        viewModelScope.launch { courseRepository.toggleChapterCompletion(userId, courseId, chapterId) }
    }

    fun deleteChapterAsOwner(chapterId: String) {
        val me = _uiState.value.currentUser?.userId ?: return
        val course = _uiState.value.course ?: return
        if (course.creatorId != me) return
        viewModelScope.launch {
            val err = courseRepository.deleteChapterForCourseOwner(chapterId, me)
            _uiState.update { it.copy(ownerActionError = err) }
        }
    }

    fun deleteCourse() {
        val course = _uiState.value.course ?: return
        val me = _uiState.value.currentUser ?: return
        if (course.creatorId != me.userId) return
        viewModelScope.launch {
            courseRepository.deleteCourseCascade(course)
            _uiState.update { it.copy(courseDeleted = true, course = null) }
        }
    }
}

//  Chapter Detail
data class ChapterDetailUiState(
    val chapter: ChapterEntity? = null,
    val currentUser: UserEntity? = null,
    val isCourseOwner: Boolean = false,
    val isEnrolled: Boolean = false,
    val isChapterCompleted: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class ChapterDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChapterDetailUiState())
    val uiState: StateFlow<ChapterDetailUiState> = _uiState.asStateFlow()

    private var chapterLoadJob: Job? = null

    fun load(chapterId: String) {
        chapterLoadJob?.cancel()
        chapterLoadJob = viewModelScope.launch {
            coroutineScope {
                launch {
                    userRepository.getCurrentUser().collect { user ->
                        _uiState.update { it.copy(currentUser = user) }
                    }
                }
                launch {
                    combine(
                        userRepository.getCurrentUser(),
                        courseRepository.getChapterById(chapterId)
                    ) { user, chapter -> user to chapter }
                        .flatMapLatest { (user, chapter) ->
                            if (chapter == null) {
                                flowOf(Pair<ChapterEntity?, Boolean>(null, false))
                            } else {
                                courseRepository.getCourseById(chapter.courseId).map { course ->
                                    val isOwner = user?.userId != null && course?.creatorId == user.userId
                                    Pair(chapter, isOwner)
                                }
                            }
                        }
                        .collect { (chapter, isOwner) ->
                            _uiState.update {
                                it.copy(chapter = chapter, isCourseOwner = isOwner, isLoading = false)
                            }
                        }
                }
                launch {
                    combine(
                        userRepository.getCurrentUser(),
                        courseRepository.getChapterById(chapterId)
                    ) { user, chapter -> user to chapter }
                        .flatMapLatest { (user, chapter) ->
                            if (chapter == null || user?.userId == null) {
                                flowOf(false to false)
                            } else {
                                val uid = user.userId
                                val chId = chapter.chapterId
                                courseRepository.getEnrollmentFlow(uid, chapter.courseId).map { enrollment ->
                                    val done = enrollment?.completedChapters
                                        ?.split(",")
                                        ?.map { it.trim() }
                                        ?.filter { it.isNotEmpty() }
                                        ?.toSet()
                                        .orEmpty()
                                    (enrollment != null) to done.contains(chId)
                                }
                            }
                        }
                        .collect { (enrolled, completed) ->
                            _uiState.update { it.copy(isEnrolled = enrolled, isChapterCompleted = completed) }
                        }
                }
            }
        }
    }

    fun toggleChapterCompletion() {
        val chapter = _uiState.value.chapter ?: return
        val uid = _uiState.value.currentUser?.userId ?: return
        viewModelScope.launch {
            courseRepository.toggleChapterCompletion(uid, chapter.courseId, chapter.chapterId)
        }
    }
}

//  Edit Course
data class EditCourseUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val durationDays: String = "",
    val imageUrl: String = "",
    val isPrivate: Boolean = false,
    val accessCode: String = "",
    val categories: List<String> = Categories.all,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditCourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseId: String = checkNotNull(savedStateHandle["courseId"])
    private val _uiState = MutableStateFlow(EditCourseUiState())
    val uiState: StateFlow<EditCourseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            courseRepository.getCourseById(courseId).collect { course ->
                if (course != null) {
                    _uiState.update {
                        it.copy(
                            title = course.title,
                            description = course.description,
                            category = course.category,
                            durationDays = if (course.durationDays > 0) course.durationDays.toString() else "",
                            imageUrl = course.imageUrl,
                            isPrivate = course.isPrivate,
                            accessCode = course.accessCode,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Course not found") }
                }
            }
        }
    }

    fun updateTitle(v: String) = _uiState.update { it.copy(title = v) }
    fun updateDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun updateCategory(v: String) = _uiState.update { it.copy(category = v) }
    fun updateDurationDays(v: String) = _uiState.update { it.copy(durationDays = v) }
    fun updateImageUrl(v: String) = _uiState.update { it.copy(imageUrl = v) }
    fun updateIsPrivate(v: Boolean) = _uiState.update { it.copy(isPrivate = v) }
    fun updateAccessCode(v: String) = _uiState.update { it.copy(accessCode = v) }

    fun save() {
        val s = _uiState.value
        if (s.title.isBlank() || s.description.isBlank()) {
            _uiState.update { it.copy(error = "Title and description are required") }
            return
        }
        if (s.isPrivate && s.accessCode.isBlank()) {
            _uiState.update { it.copy(error = "Private courses need an access code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val existing = courseRepository.getCourseByIdOnce(courseId)
            if (existing == null) {
                _uiState.update { it.copy(isSaving = false, error = "Course not found") }
                return@launch
            }
            val me = userRepository.getCurrentUserOnce()
            if (me?.userId != existing.creatorId) {
                _uiState.update { it.copy(isSaving = false, error = "You can only edit your own courses") }
                return@launch
            }
            courseRepository.updateCourse(
                existing.copy(
                    title = s.title.trim(),
                    description = s.description.trim(),
                    category = s.category,
                    imageUrl = s.imageUrl.trim(),
                    durationDays = s.durationDays.toIntOrNull() ?: 0,
                    isPrivate = s.isPrivate,
                    accessCode = if (s.isPrivate) s.accessCode.trim() else ""
                )
            )
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

//  Edit Chapter
data class EditChapterUiState(
    val title: String = "",
    val content: String = "",
    val durationMinutes: String = "",
    val mediaUrls: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditChapterViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapterId: String = checkNotNull(savedStateHandle["chapterId"])
    private val _uiState = MutableStateFlow(EditChapterUiState())
    val uiState: StateFlow<EditChapterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            courseRepository.getChapterById(chapterId).collect { ch ->
                if (ch != null) {
                    _uiState.update {
                        it.copy(
                            title = ch.title,
                            content = ch.content,
                            durationMinutes = if (ch.durationMinutes > 0) ch.durationMinutes.toString() else "",
                            mediaUrls = MediaUrlsPartition.mergeFromStorage(
                                ch.imageUrl,
                                ch.videoUrl,
                                ch.audioUrl
                            ),
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Chapter not found") }
                }
            }
        }
    }

    fun updateTitle(v: String) = _uiState.update { it.copy(title = v) }
    fun updateContent(v: String) = _uiState.update { it.copy(content = v) }
    fun updateDurationMinutes(v: String) = _uiState.update { it.copy(durationMinutes = v) }
    fun updateMediaUrls(v: String) = _uiState.update { it.copy(mediaUrls = v) }

    fun save() {
        val s = _uiState.value
        if (s.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val existing = courseRepository.getChapterByIdOnce(chapterId)
            if (existing == null) {
                _uiState.update { it.copy(isSaving = false, error = "Chapter not found") }
                return@launch
            }
            val me = userRepository.getCurrentUserOnce()
            if (me == null) {
                _uiState.update { it.copy(isSaving = false, error = "Not signed in") }
                return@launch
            }
            val (imgJoined, vidJoined, audJoined) =
                MediaUrlsPartition.splitToStorageFields(s.mediaUrls)
            val updated = existing.copy(
                title = s.title.trim(),
                content = s.content.trim(),
                durationMinutes = s.durationMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                videoUrl = vidJoined,
                audioUrl = audJoined,
                imageUrl = imgJoined
            )
            val err = courseRepository.updateChapterAsOwner(updated, me.userId)
            if (err != null) {
                _uiState.update { it.copy(isSaving = false, error = err) }
            } else {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            }
        }
    }
}
