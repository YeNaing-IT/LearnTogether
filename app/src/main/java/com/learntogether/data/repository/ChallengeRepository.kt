package com.learntogether.data.repository

import com.learntogether.data.local.dao.ChallengeDao
import com.learntogether.data.local.dao.PomodoroDao
import com.learntogether.data.local.entity.ChallengeEntity
import com.learntogether.data.local.entity.PomodoroSessionEntity
import com.learntogether.data.remote.EducationApiService
import com.learntogether.data.remote.BookSearchResponse
import com.learntogether.data.remote.QuotesApiService
import com.learntogether.data.remote.QuoteResponse
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for daily challenges and pre-designed challenge templates.
 */
@Singleton
class ChallengeRepository @Inject constructor(
    private val challengeDao: ChallengeDao
) {
    fun getTodayChallenges(userId: String): Flow<List<ChallengeEntity>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = cal.timeInMillis
        return challengeDao.getTodayChallenges(userId, startOfDay, endOfDay)
    }

    fun getAllChallenges(userId: String): Flow<List<ChallengeEntity>> =
        challengeDao.getAllChallenges(userId)

    suspend fun createChallenge(
        userId: String,
        title: String,
        description: String,
        isPreDesigned: Boolean = false
    ): ChallengeEntity {
        val challenge = ChallengeEntity(
            challengeId = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            description = description,
            isPreDesigned = isPreDesigned
        )
        challengeDao.insertChallenge(challenge)
        return challenge
    }

    suspend fun toggleChallengeComplete(challenge: ChallengeEntity) {
        challengeDao.updateChallenge(challenge.copy(isCompleted = !challenge.isCompleted))
    }

    suspend fun deleteChallenge(challenge: ChallengeEntity) = challengeDao.deleteChallenge(challenge)

    fun getCompletedChallengeCount(userId: String): Flow<Int> =
        challengeDao.getCompletedChallengeCount(userId)

    /**
     * Returns a list of pre-designed challenge templates for daily learning.
     */
    fun getPreDesignedTemplates(): List<Pair<String, String>> = listOf(
        "Read for 30 minutes" to "Pick up a book or article and read for at least 30 minutes today.",
        "Complete 3 course chapters" to "Work through three chapters from any enrolled course.",
        "Write a summary post" to "Write a post summarizing what you learned today.",
        "Help a peer" to "Answer a question or comment helpfully on someone's post.",
        "Pomodoro power hour" to "Complete two full Pomodoro sessions (50 minutes of focused learning).",
        "Explore a new topic" to "Search for and read about a subject you've never studied before.",
        "Review and revisit" to "Go back to a completed course and review one chapter.",
        "Teach what you know" to "Create a short post teaching a concept you recently learned.",
        "No distractions study" to "Do a focused study session without checking social media.",
        "Connect with learners" to "Follow 3 new users and comment on their posts."
    )
}

/**
 * Repository for pomodoro timer session tracking.
 */
@Singleton
class PomodoroRepository @Inject constructor(
    private val pomodoroDao: PomodoroDao
) {
    fun getSessions(userId: String): Flow<List<PomodoroSessionEntity>> =
        pomodoroDao.getSessions(userId)

    suspend fun saveSession(
        userId: String,
        durationMinutes: Int,
        courseId: String? = null
    ): PomodoroSessionEntity {
        val session = PomodoroSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            userId = userId,
            durationMinutes = durationMinutes,
            courseId = courseId
        )
        pomodoroDao.insertSession(session)
        return session
    }

    fun getTotalMinutes(userId: String): Flow<Int?> = pomodoroDao.getTotalMinutes(userId)

    fun getSessionCount(userId: String): Flow<Int> = pomodoroDao.getSessionCount(userId)
}

/**
 * Repository for external education API (Open Library).
 * Demonstrates internet connectivity requirement.
 */
@Singleton
class EducationApiRepository @Inject constructor(
    private val apiService: EducationApiService,
    private val quotesService: QuotesApiService
) {
    suspend fun searchBooks(query: String): Result<BookSearchResponse> {
        return try {
            Result.success(apiService.searchBooks(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubjectBooks(subject: String) = try {
        Result.success(apiService.getSubject(subject))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getDailyQuote(): Result<QuoteResponse> {
        return try {
            val quotes = quotesService.getRandomQuote()
            if (quotes.isNotEmpty()) Result.success(quotes.first())
            else Result.failure(Exception("No quotes available"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
