package com.learntogether.data.repository

import com.learntogether.data.local.dao.CourseChapterCountRow
import com.learntogether.data.local.dao.CourseDao
import com.learntogether.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for course/roadmap data operations including
 * chapters, enrollments, and chapter chat.
 *
 * **Learner progress** is persisted in Room `enrollments`: composite primary key `(userId, courseId)`,
 * so each user has a separate row (and separate [EnrollmentEntity.completedChapters]) per course.
 */
@Singleton
class CourseRepository @Inject constructor(
    private val courseDao: CourseDao
) {
    fun getAllCourses(): Flow<List<CourseEntity>> = courseDao.getAllCourses()

    fun getCourseById(courseId: String): Flow<CourseEntity?> = courseDao.getCourseById(courseId)

    suspend fun getCourseByIdOnce(courseId: String): CourseEntity? = courseDao.getCourseByIdOnce(courseId)

    fun getCoursesByCreator(userId: String): Flow<List<CourseEntity>> = courseDao.getCoursesByCreator(userId)

    fun getCoursesByCategory(category: String): Flow<List<CourseEntity>> = courseDao.getCoursesByCategory(category)

    suspend fun searchCourses(query: String): List<CourseEntity> = courseDao.searchCourses(query)

    fun getCourseCount(userId: String): Flow<Int> = courseDao.getCourseCount(userId)

    suspend fun createCourse(
        creatorId: String,
        title: String,
        description: String,
        category: String,
        imageUrl: String = "",
        durationDays: Int = 0,
        isPrivate: Boolean = false,
        accessCode: String = ""
    ): CourseEntity {
        val course = CourseEntity(
            courseId = UUID.randomUUID().toString(),
            creatorId = creatorId,
            title = title,
            description = description,
            category = category,
            imageUrl = imageUrl,
            durationDays = durationDays,
            isPrivate = isPrivate,
            accessCode = if (isPrivate) accessCode.trim() else ""
        )
        courseDao.insertCourse(course)
        return course
    }

    suspend fun updateCourse(course: CourseEntity) = courseDao.updateCourse(course)

    suspend fun getChapterCountForCourse(courseId: String): Int = courseDao.getChapterCount(courseId)

    fun observeChapterCountsByCourse(): Flow<List<CourseChapterCountRow>> =
        courseDao.observeChapterCountsByCourse()

    suspend fun deleteCourse(course: CourseEntity) = courseDao.deleteCourse(course)

    suspend fun deleteCourseCascade(course: CourseEntity) {
        val id = course.courseId
        courseDao.deleteChatMessagesForCourse(id)
        courseDao.deleteAnnouncementsForCourse(id)
        courseDao.deleteChaptersForCourse(id)
        courseDao.deleteEnrollmentsForCourse(id)
        courseDao.deleteCourse(course)
    }

    // Chapter operations
    fun getChaptersByCourse(courseId: String): Flow<List<ChapterEntity>> =
        courseDao.getChaptersByCourse(courseId)

    fun getChapterById(chapterId: String): Flow<ChapterEntity?> = courseDao.getChapterById(chapterId)

    suspend fun getChapterByIdOnce(chapterId: String): ChapterEntity? = courseDao.getChapterByIdOnce(chapterId)

    suspend fun addChapter(
        courseId: String,
        title: String,
        content: String,
        orderIndex: Int,
        videoUrl: String = "",
        audioUrl: String = "",
        imageUrl: String = "",
        durationMinutes: Int = 0
    ): ChapterEntity {
        val chapter = ChapterEntity(
            chapterId = UUID.randomUUID().toString(),
            courseId = courseId,
            title = title,
            content = content,
            orderIndex = orderIndex,
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            imageUrl = imageUrl,
            durationMinutes = durationMinutes
        )
        courseDao.insertChapter(chapter)
        return chapter
    }

    fun getAnnouncementsForCourse(courseId: String): Flow<List<CourseAnnouncementEntity>> =
        courseDao.getAnnouncementsForCourse(courseId)

    suspend fun addCourseAnnouncement(courseId: String, authorId: String, message: String): CourseAnnouncementEntity {
        val ann = CourseAnnouncementEntity(
            announcementId = UUID.randomUUID().toString(),
            courseId = courseId,
            authorId = authorId,
            message = message.trim()
        )
        courseDao.insertCourseAnnouncement(ann)
        return ann
    }

    /** Returns an error message if the acting user is not the course owner. */
    suspend fun deleteCourseAnnouncement(announcementId: String, actingUserId: String): String? {
        val ann = courseDao.getCourseAnnouncementByIdOnce(announcementId) ?: return "Announcement not found"
        val course = courseDao.getCourseByIdOnce(ann.courseId) ?: return "Course not found"
        if (course.creatorId != actingUserId) return "Only the course owner can delete announcements"
        courseDao.deleteCourseAnnouncement(ann)
        return null
    }

    suspend fun updateChapter(chapter: ChapterEntity) = courseDao.updateChapter(chapter)

    /** Updates chapter content; [actingUserId] must be the course creator. */
    suspend fun updateChapterAsOwner(updated: ChapterEntity, actingUserId: String): String? {
        val course = courseDao.getCourseByIdOnce(updated.courseId) ?: return "Course not found"
        if (course.creatorId != actingUserId) return "Only the course owner can edit chapters"
        val existing = courseDao.getChapterByIdOnce(updated.chapterId) ?: return "Chapter not found"
        if (existing.courseId != updated.courseId) return "Invalid chapter"
        courseDao.updateChapter(
            updated.copy(
                orderIndex = existing.orderIndex,
                courseId = existing.courseId,
                chapterId = existing.chapterId
            )
        )
        return null
    }

    suspend fun deleteChapter(chapter: ChapterEntity) = courseDao.deleteChapter(chapter)

    /**
     * Owner-only: removes the chapter (chat messages cascade), strips its id from all enrollments’
     * [EnrollmentEntity.completedChapters], and recomputes [EnrollmentEntity.isCompleted].
     */
    suspend fun deleteChapterForCourseOwner(chapterId: String, actingUserId: String): String? = withContext(Dispatchers.IO) {
        val chapter = courseDao.getChapterByIdOnce(chapterId) ?: return@withContext "Chapter not found"
        val course = courseDao.getCourseByIdOnce(chapter.courseId) ?: return@withContext "Course not found"
        if (course.creatorId != actingUserId) return@withContext "Only the course owner can delete chapters"
        courseDao.deleteChapter(chapter)
        val newTotal = courseDao.getChapterCount(chapter.courseId)
        val enrollments = courseDao.getEnrollmentsForCourse(chapter.courseId)
        for (e in enrollments) {
            val completed = e.completedChapters
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && it != chapterId }
                .toMutableSet()
            val isNowComplete = newTotal > 0 && completed.size >= newTotal
            courseDao.updateEnrollment(
                e.copy(
                    completedChapters = completed.joinToString(","),
                    isCompleted = isNowComplete,
                    completedAt = if (isNowComplete) System.currentTimeMillis() else null
                )
            )
        }
        null
    }

    suspend fun getChapterCount(courseId: String): Int = courseDao.getChapterCount(courseId)

    // Enrollment operations
    /**
     * @param accessCodeInput required when course is private (ignored for public). Creator never needs a code.
     * @return null on success, or an error message.
     */
    suspend fun enrollInCourseWithCode(userId: String, courseId: String, accessCodeInput: String = ""): String? {
        val course = courseDao.getCourseByIdOnce(courseId) ?: return "Course not found"
        if (course.creatorId == userId) {
            return enrollIfNeeded(userId, courseId, course)
        }
        if (course.isPrivate) {
            val expected = course.accessCode.trim()
            if (expected.isEmpty()) return "This private course has no access code"
            if (!expected.equals(accessCodeInput.trim(), ignoreCase = true)) return "Invalid access code"
        }
        return enrollIfNeeded(userId, courseId, course)
    }

    private suspend fun enrollIfNeeded(userId: String, courseId: String, course: CourseEntity): String? {
        if (courseDao.getEnrollment(userId, courseId) != null) return null
        val deadlineAt = if (course.durationDays > 0) {
            System.currentTimeMillis() + (course.durationDays.toLong() * 24 * 60 * 60 * 1000)
        } else null
        val enrollment = EnrollmentEntity(
            userId = userId,
            courseId = courseId,
            deadlineAt = deadlineAt
        )
        courseDao.insertEnrollment(enrollment)
        courseDao.incrementEnrollment(courseId)
        return null
    }

    fun isEnrolled(userId: String, courseId: String): Flow<Boolean> =
        courseDao.isEnrolled(userId, courseId)

    fun getEnrollmentFlow(userId: String, courseId: String): Flow<EnrollmentEntity?> =
        courseDao.getEnrollmentFlow(userId, courseId)

    suspend fun getEnrollment(userId: String, courseId: String): EnrollmentEntity? =
        courseDao.getEnrollment(userId, courseId)

    /** Drops the user's enrollment and decreases the course enrollment counter. */
    suspend fun unenrollFromCourse(userId: String, courseId: String): String? {
        if (courseDao.getEnrollment(userId, courseId) == null) return "You are not enrolled in this course"
        courseDao.deleteEnrollment(userId, courseId)
        courseDao.decrementEnrollmentCount(courseId)
        return null
    }

    /**
     * Persists chapter completion for this user’s enrollment only (`enrollments` row for userId + courseId).
     * Marks a chapter complete, or removes it from completed if it was already done.
     */
    suspend fun toggleChapterCompletion(userId: String, courseId: String, chapterId: String) {
        val enrollment = courseDao.getEnrollment(userId, courseId) ?: return
        val completed = enrollment.completedChapters
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        if (!completed.add(chapterId)) {
            completed.remove(chapterId)
        }
        val totalChapters = courseDao.getChapterCount(courseId)
        val isNowComplete = totalChapters > 0 && completed.size >= totalChapters
        courseDao.updateEnrollment(
            enrollment.copy(
                completedChapters = completed.joinToString(","),
                isCompleted = isNowComplete,
                completedAt = if (isNowComplete) System.currentTimeMillis() else null
            )
        )
    }

    fun getOngoingEnrollments(userId: String): Flow<List<EnrollmentEntity>> =
        courseDao.getOngoingEnrollments(userId)

    fun getCompletedEnrollments(userId: String): Flow<List<EnrollmentEntity>> =
        courseDao.getCompletedEnrollments(userId)

    fun getAllEnrollments(userId: String): Flow<List<EnrollmentEntity>> =
        courseDao.getAllEnrollments(userId)

    fun getOngoingCourseCount(userId: String): Flow<Int> = courseDao.getOngoingCourseCount(userId)

    fun getCompletedCourseCount(userId: String): Flow<Int> = courseDao.getCompletedCourseCount(userId)

    // Chat operations
    suspend fun sendChatMessage(chapterId: String, senderId: String, content: String): ChatMessageEntity {
        val message = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            chapterId = chapterId,
            senderId = senderId,
            content = content
        )
        courseDao.insertChatMessage(message)
        return message
    }

    fun getChatMessages(chapterId: String): Flow<List<ChatMessageEntity>> =
        courseDao.getChatMessages(chapterId)
}
