package com.learntogether.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user in the system.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val handle: String,        // e.g. @johndoe
    val email: String,
    val passwordHash: String,
    val bio: String = "",
    val status: String = "",
    val profilePictureUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val totalLearnedMinutes: Int = 0,
    val isCurrentUser: Boolean = false
)

/**
 * Represents a follow relationship between two users.
 */
@Entity(
    tableName = "follows",
    primaryKeys = ["followerId", "followingId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["followerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["followingId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("followerId"), Index("followingId")]
)
data class FollowEntity(
    val followerId: String,
    val followingId: String,
    val followedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a learning post (like social media post with educational content).
 */
@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["authorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("authorId")]
)
data class PostEntity(
    @PrimaryKey val postId: String,
    val authorId: String,
    val title: String,
    val content: String,       // main text/reading content
    val imageUrls: String = "", // comma-separated URLs
    val videoUrl: String = "",
    val audioUrl: String = "",
    val category: String = "",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a like on a post.
 */
@Entity(
    tableName = "post_likes",
    primaryKeys = ["userId", "postId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PostEntity::class, parentColumns = ["postId"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("userId"), Index("postId")]
)
data class PostLikeEntity(
    val userId: String,
    val postId: String,
    val likedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a comment on a post.
 */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["authorId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PostEntity::class, parentColumns = ["postId"], childColumns = ["postId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("authorId"), Index("postId")]
)
data class CommentEntity(
    @PrimaryKey val commentId: String,
    val postId: String,
    val authorId: String,
    val content: String,
    /** Null for top-level comments; set for replies. */
    val parentCommentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A like/reaction on a comment (same model for top-level comments and replies).
 */
@Entity(
    tableName = "comment_likes",
    primaryKeys = ["userId", "commentId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CommentEntity::class, parentColumns = ["commentId"], childColumns = ["commentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("userId"), Index("commentId")]
)
data class CommentLikeEntity(
    val userId: String,
    val commentId: String,
    val likedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a course/roadmap created by a user.
 */
@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["creatorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("creatorId")]
)
data class CourseEntity(
    @PrimaryKey val courseId: String,
    val creatorId: String,
    val title: String,
    val description: String,
    val category: String,
    val imageUrl: String = "",
    val durationDays: Int = 0,  // 0 means no deadline
    val isPublished: Boolean = true,
    val enrollmentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** If true, learners must enter [accessCode] to enroll (creator always has access). */
    val isPrivate: Boolean = false,
    val accessCode: String = ""
)

/**
 * Represents a chapter within a course.
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(entity = CourseEntity::class, parentColumns = ["courseId"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("courseId")]
)
data class ChapterEntity(
    @PrimaryKey val chapterId: String,
    val courseId: String,
    val title: String,
    val content: String,
    val orderIndex: Int,
    val videoUrl: String = "",
    val audioUrl: String = "",
    val imageUrl: String = "",
    val durationMinutes: Int = 0
)

/**
 * Course-wide notice from the owner. Only meaningful after the course exists ([courseId] is set).
 */
@Entity(
    tableName = "course_announcements",
    foreignKeys = [
        ForeignKey(entity = CourseEntity::class, parentColumns = ["courseId"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["authorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("courseId"), Index("authorId")]
)
data class CourseAnnouncementEntity(
    @PrimaryKey val announcementId: String,
    val courseId: String,
    val authorId: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * One row per learner per course: the canonical place **persisted in SQLite** for that user's progress.
 *
 * - Primary key `(userId, courseId)` guarantees **each user has independent progress for each course**
 *   (Alice's completion of course X never overwrites Bob's row for the same course).
 * - [completedChapters] lists which chapter IDs this user finished **for this enrollment only**.
 * - Updates go through `CourseRepository.toggleChapterCompletion` → `CourseDao.updateEnrollment`;
 *   progress is not kept only in memory.
 */
@Entity(
    tableName = "enrollments",
    primaryKeys = ["userId", "courseId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CourseEntity::class, parentColumns = ["courseId"], childColumns = ["courseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("userId"), Index("courseId")]
)
data class EnrollmentEntity(
    val userId: String,
    val courseId: String,
    val enrolledAt: Long = System.currentTimeMillis(),
    val deadlineAt: Long? = null,
    /** Comma-separated [ChapterEntity.chapterId] values completed by this user in this course. */
    val completedChapters: String = "",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

/**
 * Represents a chat message within a course chapter discussion.
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["senderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ChapterEntity::class, parentColumns = ["chapterId"], childColumns = ["chapterId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("senderId"), Index("chapterId")]
)
data class ChatMessageEntity(
    @PrimaryKey val messageId: String,
    val chapterId: String,
    val senderId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a daily challenge.
 */
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val challengeId: String,
    val userId: String,
    val title: String,
    val description: String,
    val isPreDesigned: Boolean = false,
    val isCompleted: Boolean = false,
    val targetDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a pomodoro session record.
 */
@Entity(
    tableName = "pomodoro_sessions",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("userId")]
)
data class PomodoroSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val durationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val courseId: String? = null
)
