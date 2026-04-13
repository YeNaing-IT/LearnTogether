package com.learntogether.di

import android.content.Context
import androidx.room.Room
import com.learntogether.data.local.dao.*
import com.learntogether.data.local.database.LearnTogetherDatabase
import com.learntogether.data.local.database.MIGRATION_2_3
import com.learntogether.data.local.database.MIGRATION_3_4
import com.learntogether.data.local.database.MIGRATION_4_5
import com.learntogether.data.remote.EducationApiService
import com.learntogether.data.remote.QuotesApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing database, DAO, and network dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    //  Database
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LearnTogetherDatabase {
        return Room.databaseBuilder(
            context,
            LearnTogetherDatabase::class.java,
            LearnTogetherDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(db: LearnTogetherDatabase): UserDao = db.userDao()

    @Provides
    fun providePostDao(db: LearnTogetherDatabase): PostDao = db.postDao()

    @Provides
    fun provideCourseDao(db: LearnTogetherDatabase): CourseDao = db.courseDao()

    @Provides
    fun provideChallengeDao(db: LearnTogetherDatabase): ChallengeDao = db.challengeDao()

    @Provides
    fun providePomodoroDao(db: LearnTogetherDatabase): PomodoroDao = db.pomodoroDao()

    //  Networking
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideEducationApiService(client: OkHttpClient): EducationApiService {
        return Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EducationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideQuotesApiService(client: OkHttpClient): QuotesApiService {
        return Retrofit.Builder()
            .baseUrl("https://zenquotes.io/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuotesApiService::class.java)
    }
}
