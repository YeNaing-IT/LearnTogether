package com.learntogether.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for fetching external educational content.
 * Uses the Open Library API for book/course data and quotes API for daily inspiration.
 */
interface EducationApiService {

    /**
     * Search for educational books/resources from Open Library.
     */
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year,subject,cover_i,number_of_pages_median"
    ): BookSearchResponse

    @GET("subjects/{subject}.json")
    suspend fun getSubject(
        @retrofit2.http.Path("subject") subject: String,
        @Query("limit") limit: Int = 10
    ): SubjectResponse
}

/**
 * Quotes API for daily motivation/challenges.
 */
interface QuotesApiService {
    @GET("random")
    suspend fun getRandomQuote(): List<QuoteResponse>
}

//  Response Models
data class BookSearchResponse(
    val numFound: Int = 0,
    val docs: List<BookDoc> = emptyList()
)

data class BookDoc(
    val key: String = "",
    val title: String = "",
    val author_name: List<String>? = null,
    val first_publish_year: Int? = null,
    val subject: List<String>? = null,
    val cover_i: Int? = null,
    val number_of_pages_median: Int? = null
) {
    val coverUrl: String
        get() = if (cover_i != null) "https://covers.openlibrary.org/b/id/$cover_i-M.jpg" else ""

    val authorDisplay: String
        get() = author_name?.firstOrNull() ?: "Unknown Author"
}

data class SubjectResponse(
    val name: String = "",
    val work_count: Int = 0,
    val works: List<SubjectWork> = emptyList()
)

data class SubjectWork(
    val key: String = "",
    val title: String = "",
    val authors: List<SubjectAuthor>? = null,
    val cover_id: Int? = null
) {
    val coverUrl: String
        get() = if (cover_id != null) "https://covers.openlibrary.org/b/id/$cover_id-M.jpg" else ""
}

data class SubjectAuthor(
    val name: String = ""
)

data class QuoteResponse(
    val q: String = "",
    val a: String = ""
)
