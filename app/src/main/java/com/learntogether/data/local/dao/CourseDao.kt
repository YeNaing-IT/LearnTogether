package com.learntogether.data.local.dao

import androidx.room.*
import com.learntogether.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/** One row per course that has at least one chapter (courses with zero chapters are omitted). */
data class CourseChapterCountRow(
    val courseId: String,
    val chapterCount: Int
)

@Dao
interface CourseDao {
    /** All courses for discovery; private ones still require access code on enroll (see app logic). */
    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE courseId = :courseId")
    fun getCourseById(courseId: String): Flow<CourseEntity?>

    @Query("SELECT * FROM courses WHERE courseId = :courseId")
    suspend fun getCourseByIdOnce(courseId: String): CourseEntity?

    @Query("SELECT * FROM courses WHERE creatorId = :userId ORDER BY createdAt DESC")
    fun getCoursesByCreator(userId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE category LIKE '%' || :category || '%' ORDER BY createdAt DESC")
    fun getCoursesByCategory(category: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchCourses(query: String): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM chat_messages WHERE chapterId IN (SELECT chapterId FROM chapters WHERE courseId = :courseId)")
    suspend fun deleteChatMessagesForCourse(courseId: String)

    @Query("DELETE FROM chapters WHERE courseId = :courseId")
    suspend fun deleteChaptersForCourse(courseId: String)

    @Query("DELETE FROM enrollments WHERE courseId = :courseId")
    suspend fun deleteEnrollmentsForCourse(courseId: String)

    @Query("DELETE FROM course_announcements WHERE courseId = :courseId")
    suspend fun deleteAnnouncementsForCourse(courseId: String)

    @Query("UPDATE courses SET enrollmentCount = enrollmentCount + 1 WHERE courseId = :courseId")
    suspend fun incrementEnrollment(courseId: String)

    @Query("UPDATE courses SET enrollmentCount = MAX(0, enrollmentCount - 1) WHERE courseId = :courseId")
    suspend fun decrementEnrollmentCount(courseId: String)

    @Query("DELETE FROM enrollments WHERE userId = :userId AND courseId = :courseId")
    suspend fun deleteEnrollment(userId: String, courseId: String)

    @Query("SELECT COUNT(*) FROM courses WHERE creatorId = :userId")
    fun getCourseCount(userId: String): Flow<Int>

    // Chapter queries
    @Query("SELECT * FROM chapters WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getChaptersByCourse(courseId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE chapterId = :chapterId")
    fun getChapterById(chapterId: String): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE chapterId = :chapterId")
    suspend fun getChapterByIdOnce(chapterId: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("SELECT COUNT(*) FROM chapters WHERE courseId = :courseId")
    suspend fun getChapterCount(courseId: String): Int

    /** Emits whenever the `chapters` table changes, so list UIs stay in sync after add/remove. */
    @Query("SELECT courseId, COUNT(*) AS chapterCount FROM chapters GROUP BY courseId")
    fun observeChapterCountsByCourse(): Flow<List<CourseChapterCountRow>>

    @Query("SELECT * FROM course_announcements WHERE courseId = :courseId ORDER BY createdAt DESC")
    fun getAnnouncementsForCourse(courseId: String): Flow<List<CourseAnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseAnnouncement(announcement: CourseAnnouncementEntity)

    @Delete
    suspend fun deleteCourseAnnouncement(announcement: CourseAnnouncementEntity)

    @Query("SELECT * FROM course_announcements WHERE announcementId = :id LIMIT 1")
    suspend fun getCourseAnnouncementByIdOnce(id: String): CourseAnnouncementEntity?

    // Enrollment queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: EnrollmentEntity)

    @Update
    suspend fun updateEnrollment(enrollment: EnrollmentEntity)

    @Query("SELECT * FROM enrollments WHERE userId = :userId AND courseId = :courseId")
    suspend fun getEnrollment(userId: String, courseId: String): EnrollmentEntity?

    @Query("SELECT * FROM enrollments WHERE courseId = :courseId")
    suspend fun getEnrollmentsForCourse(courseId: String): List<EnrollmentEntity>

    @Query("SELECT * FROM enrollments WHERE userId = :userId AND courseId = :courseId")
    fun getEnrollmentFlow(userId: String, courseId: String): Flow<EnrollmentEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM enrollments WHERE userId = :userId AND courseId = :courseId)")
    fun isEnrolled(userId: String, courseId: String): Flow<Boolean>

    @Query("SELECT * FROM enrollments WHERE userId = :userId AND isCompleted = 0 ORDER BY enrolledAt DESC")
    fun getOngoingEnrollments(userId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedEnrollments(userId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE userId = :userId ORDER BY enrolledAt DESC")
    fun getAllEnrollments(userId: String): Flow<List<EnrollmentEntity>>

    @Query("SELECT COUNT(*) FROM enrollments WHERE userId = :userId AND isCompleted = 0")
    fun getOngoingCourseCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM enrollments WHERE userId = :userId AND isCompleted = 1")
    fun getCompletedCourseCount(userId: String): Flow<Int>

    // Chat queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE chapterId = :chapterId ORDER BY createdAt ASC")
    fun getChatMessages(chapterId: String): Flow<List<ChatMessageEntity>>
}
