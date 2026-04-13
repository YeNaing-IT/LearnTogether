package com.learntogether.data.local.dao

import androidx.room.*
import com.learntogether.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE userId = :userId AND targetDate >= :startOfDay AND targetDate < :endOfDay ORDER BY createdAt ASC")
    fun getTodayChallenges(userId: String, startOfDay: Long, endOfDay: Long): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE userId = :userId ORDER BY targetDate DESC")
    fun getAllChallenges(userId: String): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE isPreDesigned = 1")
    fun getPreDesignedChallenges(): Flow<List<ChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity)

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)

    @Delete
    suspend fun deleteChallenge(challenge: ChallengeEntity)

    @Query("SELECT COUNT(*) FROM challenges WHERE userId = :userId AND isCompleted = 1")
    fun getCompletedChallengeCount(userId: String): Flow<Int>
}

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions WHERE userId = :userId ORDER BY completedAt DESC")
    fun getSessions(userId: String): Flow<List<PomodoroSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSessionEntity)

    @Query("SELECT SUM(durationMinutes) FROM pomodoro_sessions WHERE userId = :userId")
    fun getTotalMinutes(userId: String): Flow<Int?>

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE userId = :userId")
    fun getSessionCount(userId: String): Flow<Int>
}
