package com.learntogether

import com.learntogether.data.local.entity.*
import com.learntogether.util.Categories
import com.learntogether.util.TimeUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for model logic and utility functions.
 * Validates TimeUtils, entity creation, and Categories.
 */
class TimeUtilsTest {

    @Test
    fun `getTimeAgo returns Just now for recent timestamps`() {
        val now = System.currentTimeMillis()
        assertEquals("Just now", TimeUtils.getTimeAgo(now))
        assertEquals("Just now", TimeUtils.getTimeAgo(now - 30_000)) // 30 seconds ago
    }

    @Test
    fun `getTimeAgo returns minutes for timestamps within an hour`() {
        val now = System.currentTimeMillis()
        val fiveMinAgo = now - TimeUnit.MINUTES.toMillis(5)
        assertEquals("5m ago", TimeUtils.getTimeAgo(fiveMinAgo))
    }

    @Test
    fun `getTimeAgo returns hours for timestamps within a day`() {
        val now = System.currentTimeMillis()
        val threeHoursAgo = now - TimeUnit.HOURS.toMillis(3)
        assertEquals("3h ago", TimeUtils.getTimeAgo(threeHoursAgo))
    }

    @Test
    fun `getTimeAgo returns days for timestamps within a week`() {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)
        assertEquals("2d ago", TimeUtils.getTimeAgo(twoDaysAgo))
    }

    @Test
    fun `getTimeAgo returns weeks for timestamps within a month`() {
        val now = System.currentTimeMillis()
        val twoWeeksAgo = now - TimeUnit.DAYS.toMillis(14)
        assertEquals("2w ago", TimeUtils.getTimeAgo(twoWeeksAgo))
    }

    @Test
    fun `formatMinutes correctly formats time`() {
        assertEquals("0m", TimeUtils.formatMinutes(0))
        assertEquals("30m", TimeUtils.formatMinutes(30))
        assertEquals("1h", TimeUtils.formatMinutes(60))
        assertEquals("1h 30m", TimeUtils.formatMinutes(90))
        assertEquals("2h", TimeUtils.formatMinutes(120))
        assertEquals("10h 5m", TimeUtils.formatMinutes(605))
    }

    @Test
    fun `getDaysRemaining returns correct days`() {
        val now = System.currentTimeMillis()
        val fiveDaysFromNow = now + TimeUnit.DAYS.toMillis(5)
        assertEquals(5, TimeUtils.getDaysRemaining(fiveDaysFromNow))
    }

    @Test
    fun `getDaysRemaining returns 0 for past deadlines`() {
        val now = System.currentTimeMillis()
        val yesterday = now - TimeUnit.DAYS.toMillis(1)
        assertEquals(0, TimeUtils.getDaysRemaining(yesterday))
    }

    @Test
    fun `formatDate returns non-empty string`() {
        val timestamp = System.currentTimeMillis()
        val formatted = TimeUtils.formatDate(timestamp)
        assertTrue(formatted.isNotBlank())
    }
}

class EntityTest {

    @Test
    fun `UserEntity default values are correct`() {
        val user = UserEntity(
            userId = "test-id",
            username = "Test User",
            handle = "testuser",
            email = "test@example.com",
            passwordHash = "hash"
        )
        assertEquals("", user.bio)
        assertEquals("", user.status)
        assertEquals("", user.profilePictureUrl)
        assertEquals(0, user.totalLearnedMinutes)
        assertFalse(user.isCurrentUser)
        assertTrue(user.createdAt > 0)
    }

    @Test
    fun `PostEntity default values are correct`() {
        val post = PostEntity(
            postId = "post-1",
            authorId = "user-1",
            title = "Test Post",
            content = "Content"
        )
        assertEquals("", post.imageUrls)
        assertEquals("", post.videoUrl)
        assertEquals("", post.audioUrl)
        assertEquals("", post.category)
        assertEquals(0, post.likeCount)
        assertEquals(0, post.commentCount)
    }

    @Test
    fun `CourseEntity default values are correct`() {
        val course = CourseEntity(
            courseId = "course-1",
            creatorId = "user-1",
            title = "Test Course",
            description = "Description",
            category = "Programming"
        )
        assertEquals("", course.imageUrl)
        assertEquals(0, course.durationDays)
        assertTrue(course.isPublished)
        assertEquals(0, course.enrollmentCount)
        assertFalse(course.isPrivate)
        assertEquals("", course.accessCode)
    }

    @Test
    fun `EnrollmentEntity deadline calculation logic`() {
        val durationDays = 90
        val enrolledAt = System.currentTimeMillis()
        val deadlineAt = enrolledAt + (durationDays.toLong() * 24 * 60 * 60 * 1000)

        val enrollment = EnrollmentEntity(
            userId = "user-1",
            courseId = "course-1",
            enrolledAt = enrolledAt,
            deadlineAt = deadlineAt
        )

        assertNotNull(enrollment.deadlineAt)
        assertTrue(enrollment.deadlineAt!! > enrollment.enrolledAt)
        val expectedDays = TimeUtils.getDaysRemaining(enrollment.deadlineAt!!)
        assertTrue(expectedDays in 89..90) // Allow small variance
    }

    @Test
    fun `EnrollmentEntity completed chapters parsing`() {
        val enrollment = EnrollmentEntity(
            userId = "user-1",
            courseId = "course-1",
            completedChapters = "ch1,ch2,ch3"
        )
        val completedIds = enrollment.completedChapters.split(",").filter { it.isNotBlank() }
        assertEquals(3, completedIds.size)
        assertTrue(completedIds.contains("ch1"))
        assertTrue(completedIds.contains("ch2"))
        assertTrue(completedIds.contains("ch3"))
    }

    @Test
    fun `EnrollmentEntity empty completed chapters`() {
        val enrollment = EnrollmentEntity(
            userId = "user-1",
            courseId = "course-1",
            completedChapters = ""
        )
        val completedIds = enrollment.completedChapters.split(",").filter { it.isNotBlank() }
        assertEquals(0, completedIds.size)
    }

    @Test
    fun `ChallengeEntity default values`() {
        val challenge = ChallengeEntity(
            challengeId = "ch-1",
            userId = "user-1",
            title = "Read 30 min",
            description = "Read a book"
        )
        assertFalse(challenge.isPreDesigned)
        assertFalse(challenge.isCompleted)
    }

    @Test
    fun `ChapterEntity ordering`() {
        val chapters = listOf(
            ChapterEntity(chapterId = "c3", courseId = "course-1", title = "C", content = "", orderIndex = 2),
            ChapterEntity(chapterId = "c1", courseId = "course-1", title = "A", content = "", orderIndex = 0),
            ChapterEntity(chapterId = "c2", courseId = "course-1", title = "B", content = "", orderIndex = 1)
        )
        val sorted = chapters.sortedBy { it.orderIndex }
        assertEquals("c1", sorted[0].chapterId)
        assertEquals("c2", sorted[1].chapterId)
        assertEquals("c3", sorted[2].chapterId)
    }
}

class CategoriesTest {

    @Test
    fun `categories list is not empty`() {
        assertTrue(Categories.all.isNotEmpty())
    }

    @Test
    fun `categories contains expected items`() {
        assertTrue(Categories.all.contains("Programming"))
        assertTrue(Categories.all.contains("Mathematics"))
        assertTrue(Categories.all.contains("Other"))
    }

    @Test
    fun `categories has no duplicates`() {
        assertEquals(Categories.all.size, Categories.all.toSet().size)
    }
}

class PostImageParsingTest {

    @Test
    fun `parse comma-separated image URLs`() {
        val imageUrls = "https://example.com/1.jpg,https://example.com/2.jpg,https://example.com/3.jpg"
        val urls = imageUrls.split(",").filter { it.isNotBlank() }
        assertEquals(3, urls.size)
    }

    @Test
    fun `parse empty image URLs`() {
        val imageUrls = ""
        val urls = imageUrls.split(",").filter { it.isNotBlank() }
        assertEquals(0, urls.size)
    }

    @Test
    fun `parse single image URL`() {
        val imageUrls = "https://example.com/1.jpg"
        val urls = imageUrls.split(",").filter { it.isNotBlank() }
        assertEquals(1, urls.size)
    }
}

class PasswordHashTest {

    @Test
    fun `hash produces consistent output`() {
        val password = "testPassword123"
        val hash1 = hashPassword(password)
        val hash2 = hashPassword(password)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `different passwords produce different hashes`() {
        val hash1 = hashPassword("password1")
        val hash2 = hashPassword("password2")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `hash is 64 characters (SHA-256 hex)`() {
        val hash = hashPassword("test")
        assertEquals(64, hash.length)
    }

    private fun hashPassword(password: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
