package com.learntogether.data.local.dao

import androidx.room.*
import com.learntogether.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/** Row for [PostDao.observeCommentLikeAggregates]. */
data class CommentLikeAggregate(
    val commentId: String,
    val likeCount: Int
)

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE authorId = :userId ORDER BY createdAt DESC")
    fun getPostsByUser(userId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE postId = :postId")
    fun getPostById(postId: String): Flow<PostEntity?>

    @Query("SELECT * FROM posts WHERE postId = :postId")
    suspend fun getPostByIdOnce(postId: String): PostEntity?

    @Query("""
        SELECT p.* FROM posts p 
        INNER JOIN follows f ON p.authorId = f.followingId 
        WHERE f.followerId = :userId 
        ORDER BY p.createdAt DESC
    """)
    fun getFeedPosts(userId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE category LIKE '%' || :category || '%' ORDER BY createdAt DESC")
    fun getPostsByCategory(category: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchPosts(query: String): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("UPDATE posts SET likeCount = likeCount + 1 WHERE postId = :postId")
    suspend fun incrementLikeCount(postId: String)

    @Query("UPDATE posts SET likeCount = MAX(0, likeCount - 1) WHERE postId = :postId")
    suspend fun decrementLikeCount(postId: String)

    @Query("UPDATE posts SET commentCount = commentCount + 1 WHERE postId = :postId")
    suspend fun incrementCommentCount(postId: String)

    // Like operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun likePost(like: PostLikeEntity)

    @Delete
    suspend fun unlikePost(like: PostLikeEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM post_likes WHERE userId = :userId AND postId = :postId)")
    fun isPostLiked(userId: String, postId: String): Flow<Boolean>

    @Query("SELECT p.* FROM posts p INNER JOIN post_likes pl ON p.postId = pl.postId WHERE pl.userId = :userId ORDER BY pl.likedAt DESC")
    fun getLikedPosts(userId: String): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) FROM post_likes WHERE userId = :userId")
    fun getLikedPostCount(userId: String): Flow<Int>

    @Query("SELECT postId FROM post_likes WHERE userId = :userId AND postId IN (:postIds)")
    suspend fun getLikedPostIdsInList(userId: String, postIds: List<String>): List<String>

    // Comment operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun getCommentsByPost(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    suspend fun getCommentsByPostOnce(postId: String): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE commentId = :commentId")
    suspend fun getCommentByIdOnce(commentId: String): CommentEntity?

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: String)

    @Query("DELETE FROM comment_likes WHERE commentId IN (SELECT commentId FROM comments WHERE postId = :postId)")
    suspend fun deleteCommentLikesForPost(postId: String)

    @Query("DELETE FROM comment_likes WHERE commentId IN (:commentIds)")
    suspend fun deleteCommentLikesForComments(commentIds: List<String>)

    @Query("DELETE FROM comments WHERE commentId IN (:commentIds)")
    suspend fun deleteCommentsByIds(commentIds: List<String>)

    @Query("UPDATE posts SET commentCount = MAX(0, commentCount - :amount) WHERE postId = :postId")
    suspend fun decrementCommentCountBy(postId: String, amount: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentLike(like: CommentLikeEntity)

    @Delete
    suspend fun deleteCommentLike(like: CommentLikeEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM comment_likes WHERE userId = :userId AND commentId = :commentId)")
    suspend fun isCommentLikedByUser(userId: String, commentId: String): Boolean

    @Query(
        """
        SELECT cl.commentId AS commentId, COUNT(*) AS likeCount FROM comment_likes cl
        INNER JOIN comments c ON c.commentId = cl.commentId
        WHERE c.postId = :postId
        GROUP BY cl.commentId
        """
    )
    fun observeCommentLikeAggregates(postId: String): Flow<List<CommentLikeAggregate>>

    @Query(
        """
        SELECT cl.commentId FROM comment_likes cl
        INNER JOIN comments c ON c.commentId = cl.commentId
        WHERE c.postId = :postId AND cl.userId = :userId
        """
    )
    fun observeLikedCommentIdsForPost(userId: String, postId: String): Flow<List<String>>

    @Query("DELETE FROM post_likes WHERE postId = :postId")
    suspend fun deleteLikesForPost(postId: String)

    @Query("SELECT COUNT(*) FROM posts WHERE authorId = :userId")
    fun getPostCount(userId: String): Flow<Int>
}
