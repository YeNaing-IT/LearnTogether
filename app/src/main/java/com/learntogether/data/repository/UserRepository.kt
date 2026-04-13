package com.learntogether.data.repository

import android.content.Context
import android.net.Uri
import com.learntogether.data.local.dao.UserDao
import com.learntogether.data.local.database.LearnTogetherDatabase
import com.learntogether.data.local.entity.FollowEntity
import com.learntogether.data.local.entity.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user-related data operations.
 * Acts as single source of truth following the Repository pattern.
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val database: LearnTogetherDatabase,
    @ApplicationContext private val context: Context
) {
    fun getCurrentUser(): Flow<UserEntity?> = userDao.getCurrentUser()

    suspend fun getCurrentUserOnce(): UserEntity? = userDao.getCurrentUserOnce()

    fun getUserById(userId: String): Flow<UserEntity?> = userDao.getUserById(userId)

    suspend fun getUserByIdOnce(userId: String): UserEntity? = userDao.getUserByIdOnce(userId)

    suspend fun searchUsers(query: String): List<UserEntity> = userDao.searchUsers(query)

    suspend fun register(username: String, handle: String, email: String, password: String): Result<UserEntity> {
        return try {
            val user = UserEntity(
                userId = UUID.randomUUID().toString(),
                username = username.trim(),
                handle = normalizeHandle(handle),
                email = email.trim(),
                passwordHash = hashPassword(password),
                isCurrentUser = true
            )
            userDao.clearCurrentUser()
            userDao.insertUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        return try {
            val user = userDao.login(email, hashPassword(password))
            if (user != null) {
                // Do not use insertUser(REPLACE) here: SQLite REPLACE deletes the old row first,
                // which triggers FK CASCADE and wipes this user's posts, courses, follows, etc.
                userDao.switchCurrentUserTo(user.userId)
                val current = userDao.getUserByIdOnce(user.userId)
                Result.success(current ?: user.copy(isCurrentUser = true))
            } else {
                Result.failure(Exception("Invalid email or password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        userDao.clearCurrentUser()
    }

    suspend fun updateProfile(user: UserEntity) {
        userDao.updateUser(user)
    }

    /**
     * Copies a picked gallery image into app storage and returns the absolute file path for [UserEntity.profilePictureUrl].
     */
    suspend fun importProfileImageFromUri(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getCurrentUserOnce() ?: return@withContext Result.failure(Exception("Not signed in"))
            val dir = File(context.filesDir, "profile_images").apply { mkdirs() }
            val dest = File(dir, "${user.userId}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(Exception("Could not read image"))
            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates profile photo (absolute path, https URL, or empty) and bio for the signed-in user.
     */
    suspend fun updateCurrentUserProfileFields(
        profilePictureUrl: String,
        bio: String,
        status: String
    ): Result<Unit> {
        val user = userDao.getCurrentUserOnce() ?: return Result.failure(Exception("Not signed in"))
        val trimmedPath = profilePictureUrl.trim()
        if (trimmedPath.isEmpty()) {
            withContext(Dispatchers.IO) {
                File(context.filesDir, "profile_images/${user.userId}.jpg").takeIf { it.exists() }?.delete()
            }
        }
        userDao.updateUser(
            user.copy(
                profilePictureUrl = trimmedPath,
                bio = bio.trim(),
                status = status.trim()
            )
        )
        return Result.success(Unit)
    }

    /**
     * Normalizes handle to a single stored form: `@localpart` (lowercase local part).
     */
    fun normalizeHandle(raw: String): String {
        val local = raw.trim().removePrefix("@").trim().lowercase()
        return if (local.isEmpty()) "" else "@$local"
    }

    suspend fun updateCurrentUserEmail(newEmail: String, currentPassword: String): Result<Unit> {
        val user = userDao.getCurrentUserOnce() ?: return Result.failure(Exception("Not signed in"))
        if (user.passwordHash != hashPassword(currentPassword)) {
            return Result.failure(Exception("Incorrect password"))
        }
        val email = newEmail.trim()
        if (email.isBlank()) return Result.failure(Exception("Email cannot be empty"))
        if (!email.contains('@') || email.length < 5) {
            return Result.failure(Exception("Enter a valid email address"))
        }
        if (userDao.countOtherUsersWithEmail(email, user.userId) > 0) {
            return Result.failure(Exception("This email is already in use"))
        }
        userDao.updateUser(user.copy(email = email))
        return Result.success(Unit)
    }

    suspend fun updateCurrentUserUsername(newUsername: String, currentPassword: String): Result<Unit> {
        val user = userDao.getCurrentUserOnce() ?: return Result.failure(Exception("Not signed in"))
        if (user.passwordHash != hashPassword(currentPassword)) {
            return Result.failure(Exception("Incorrect password"))
        }
        val name = newUsername.trim()
        if (name.isBlank()) return Result.failure(Exception("Name cannot be empty"))
        if (name.length < 2) return Result.failure(Exception("Name must be at least 2 characters"))
        if (userDao.countOtherUsersWithUsername(name, user.userId) > 0) {
            return Result.failure(Exception("This display name is already taken"))
        }
        userDao.updateUser(user.copy(username = name))
        return Result.success(Unit)
    }

    suspend fun updateCurrentUserHandle(newHandle: String, currentPassword: String): Result<Unit> {
        val user = userDao.getCurrentUserOnce() ?: return Result.failure(Exception("Not signed in"))
        if (user.passwordHash != hashPassword(currentPassword)) {
            return Result.failure(Exception("Incorrect password"))
        }
        val handle = normalizeHandle(newHandle)
        if (handle.length < 2) return Result.failure(Exception("Handle cannot be empty"))
        val localPart = handle.removePrefix("@")
        if (!localPart.all { it.isLetterOrDigit() || it == '_' }) {
            return Result.failure(Exception("Handle may only contain letters, numbers, and underscores"))
        }
        if (userDao.countOtherUsersWithHandle(handle, user.userId) > 0) {
            return Result.failure(Exception("This handle is already taken"))
        }
        userDao.updateUser(user.copy(handle = handle))
        return Result.success(Unit)
    }

    suspend fun deleteCurrentUserAccount(password: String): Result<Unit> {
        val user = userDao.getCurrentUserOnce() ?: return Result.failure(Exception("Not signed in"))
        if (user.passwordHash != hashPassword(password)) {
            return Result.failure(Exception("Incorrect password"))
        }
        userDao.deleteUserAccount(user.userId)
        return Result.success(Unit)
    }

    suspend fun addLearnedMinutes(userId: String, minutes: Int) {
        userDao.addLearnedMinutes(userId, minutes)
    }

    // Follow operations
    suspend fun followUser(currentUserId: String, targetUserId: String) {
        userDao.followUser(FollowEntity(followerId = currentUserId, followingId = targetUserId))
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        userDao.unfollowUser(FollowEntity(followerId = currentUserId, followingId = targetUserId))
    }

    fun getFollowingCount(userId: String): Flow<Int> = userDao.getFollowingCount(userId)

    fun getFollowerCount(userId: String): Flow<Int> = userDao.getFollowerCount(userId)

    fun isFollowing(currentUserId: String, targetUserId: String): Flow<Boolean> =
        userDao.isFollowing(currentUserId, targetUserId)

    fun getFollowingUsers(userId: String): Flow<List<UserEntity>> = userDao.getFollowingUsers(userId)

    fun getFollowers(userId: String): Flow<List<UserEntity>> = userDao.getFollowers(userId)

    /**
     * Removes all rows from every Room table (users, posts, courses, chat, etc.) and deletes
     * locally stored profile images. Use for a full reset of on-device data.
     */
    suspend fun wipeAllLocalDatabaseData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            val dir = File(context.filesDir, "profile_images")
            if (dir.isDirectory) dir.listFiles()?.forEach { it.delete() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
