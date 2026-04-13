package com.learntogether.data.local.dao

import androidx.room.*
import com.learntogether.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserOnce(): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserByIdOnce(userId: String): UserEntity?

    @Query(
        """
        SELECT * FROM users WHERE
        LOWER(REPLACE(handle, '@', '')) LIKE '%' || LOWER(REPLACE(:query, '@', '')) || '%'
        OR LOWER(username) LIKE '%' || LOWER(:query) || '%'
        """
    )
    suspend fun searchUsers(query: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUser()

    /** Marks one row as the session user. Call after [clearCurrentUser]. */
    @Query("UPDATE users SET isCurrentUser = 1 WHERE userId = :userId")
    suspend fun setUserAsCurrent(userId: String)

    @Transaction
    suspend fun switchCurrentUserTo(userId: String) {
        clearCurrentUser()
        setUserAsCurrent(userId)
    }

    @Query("UPDATE users SET totalLearnedMinutes = totalLearnedMinutes + :minutes WHERE userId = :userId")
    suspend fun addLearnedMinutes(userId: String, minutes: Int)

    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(email: String, passwordHash: String): UserEntity?

    // Follow operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followUser(follow: FollowEntity)

    @Delete
    suspend fun unfollowUser(follow: FollowEntity)

    @Query("SELECT COUNT(*) FROM follows WHERE followerId = :userId")
    fun getFollowingCount(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM follows WHERE followingId = :userId")
    fun getFollowerCount(userId: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :currentUserId AND followingId = :targetUserId)")
    fun isFollowing(currentUserId: String, targetUserId: String): Flow<Boolean>

    @Query("""
        SELECT u.* FROM users u 
        INNER JOIN follows f ON u.userId = f.followingId 
        WHERE f.followerId = :userId
    """)
    fun getFollowingUsers(userId: String): Flow<List<UserEntity>>

    @Query("""
        SELECT u.* FROM users u 
        INNER JOIN follows f ON u.userId = f.followerId 
        WHERE f.followingId = :userId
    """)
    fun getFollowers(userId: String): Flow<List<UserEntity>>

    @Query("DELETE FROM challenges WHERE userId = :userId")
    suspend fun deleteChallengesForUser(userId: String)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)

    @Transaction
    suspend fun deleteUserAccount(userId: String) {
        deleteChallengesForUser(userId)
        deleteUserById(userId)
    }

    @Query(
        "SELECT COUNT(*) FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) AND userId != :excludeUserId"
    )
    suspend fun countOtherUsersWithEmail(email: String, excludeUserId: String): Int

    @Query(
        "SELECT COUNT(*) FROM users WHERE LOWER(TRIM(username)) = LOWER(TRIM(:username)) AND userId != :excludeUserId"
    )
    suspend fun countOtherUsersWithUsername(username: String, excludeUserId: String): Int

    @Query(
        "SELECT COUNT(*) FROM users WHERE LOWER(TRIM(REPLACE(handle, '@', ''))) = LOWER(TRIM(REPLACE(:handle, '@', ''))) AND userId != :excludeUserId"
    )
    suspend fun countOtherUsersWithHandle(handle: String, excludeUserId: String): Int
}
