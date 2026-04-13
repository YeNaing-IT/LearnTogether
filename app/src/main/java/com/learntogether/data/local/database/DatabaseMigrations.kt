package com.learntogether.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE comments ADD COLUMN parentCommentId TEXT DEFAULT NULL")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE courses ADD COLUMN accessCode TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `comment_likes` (
                `userId` TEXT NOT NULL,
                `commentId` TEXT NOT NULL,
                `likedAt` INTEGER NOT NULL,
                PRIMARY KEY(`userId`, `commentId`),
                FOREIGN KEY(`userId`) REFERENCES `users`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`commentId`) REFERENCES `comments`(`commentId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comment_likes_userId` ON `comment_likes` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comment_likes_commentId` ON `comment_likes` (`commentId`)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `course_announcements` (
                `announcementId` TEXT NOT NULL,
                `courseId` TEXT NOT NULL,
                `authorId` TEXT NOT NULL,
                `message` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`announcementId`),
                FOREIGN KEY(`courseId`) REFERENCES `courses`(`courseId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`authorId`) REFERENCES `users`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_course_announcements_courseId` ON `course_announcements` (`courseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_course_announcements_authorId` ON `course_announcements` (`authorId`)")
    }
}
