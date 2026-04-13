package com.learntogether.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.learntogether.data.local.dao.*
import com.learntogether.data.local.entity.*

/**
 * Main Room database for LearnTogether.
 * Contains all tables for users, posts, courses, chapters,
 * enrollments (per-user per-course progress in `enrollments`),
 * chat messages, challenges, and pomodoro sessions.
 */
@Database(
    entities = [
        UserEntity::class,
        FollowEntity::class,
        PostEntity::class,
        PostLikeEntity::class,
        CommentEntity::class,
        CommentLikeEntity::class,
        CourseEntity::class,
        ChapterEntity::class,
        EnrollmentEntity::class,
        CourseAnnouncementEntity::class,
        ChatMessageEntity::class,
        ChallengeEntity::class,
        PomodoroSessionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class LearnTogetherDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun courseDao(): CourseDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun pomodoroDao(): PomodoroDao

    companion object {
        const val DATABASE_NAME = "learn_together_db"
    }
}
