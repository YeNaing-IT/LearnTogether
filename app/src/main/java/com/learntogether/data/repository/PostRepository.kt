package com.learntogether.data.repository

import com.learntogether.data.local.dao.CommentLikeAggregate
import com.learntogether.data.local.dao.PostDao
import com.learntogether.data.local.entity.CommentEntity
import com.learntogether.data.local.entity.CommentLikeEntity
import com.learntogether.data.local.entity.PostEntity
import com.learntogether.data.local.entity.PostLikeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for post-related data operations including
 * likes, comments, and feed management.
 */
@Singleton
class PostRepository @Inject constructor(
    private val postDao: PostDao
) {
    fun getAllPosts(): Flow<List<PostEntity>> = postDao.getAllPosts()

    fun getPostsByUser(userId: String): Flow<List<PostEntity>> = postDao.getPostsByUser(userId)

    fun getPostById(postId: String): Flow<PostEntity?> = postDao.getPostById(postId)

    suspend fun getPostByIdOnce(postId: String): PostEntity? = postDao.getPostByIdOnce(postId)

    fun getFeedPosts(userId: String): Flow<List<PostEntity>> = postDao.getFeedPosts(userId)

    fun getPostsByCategory(category: String): Flow<List<PostEntity>> = postDao.getPostsByCategory(category)

    suspend fun searchPosts(query: String): List<PostEntity> = postDao.searchPosts(query)

    suspend fun createPost(
        authorId: String,
        title: String,
        content: String,
        imageUrls: List<String> = emptyList(),
        videoUrl: String = "",
        audioUrl: String = "",
        category: String = ""
    ): PostEntity {
        val post = PostEntity(
            postId = UUID.randomUUID().toString(),
            authorId = authorId,
            title = title,
            content = content,
            imageUrls = imageUrls.joinToString(","),
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            category = category
        )
        postDao.insertPost(post)
        return post
    }

    suspend fun updatePost(post: PostEntity) = postDao.updatePost(post)

    suspend fun deletePost(post: PostEntity) {
        postDao.deleteCommentLikesForPost(post.postId)
        postDao.deleteCommentsForPost(post.postId)
        postDao.deleteLikesForPost(post.postId)
        postDao.deletePost(post)
    }

    suspend fun getLikedPostIdsForUser(userId: String, postIds: List<String>): Set<String> {
        if (postIds.isEmpty()) return emptySet()
        return postDao.getLikedPostIdsInList(userId, postIds).toSet()
    }

    // Like operations
    suspend fun toggleLike(userId: String, postId: String): Boolean {
        val post = postDao.getPostByIdOnce(postId) ?: return false
        val like = PostLikeEntity(userId = userId, postId = postId)
        // Simple toggle: try to check existence via a direct query
        return try {
            postDao.likePost(like)
            postDao.incrementLikeCount(postId)
            true
        } catch (e: Exception) {
            postDao.unlikePost(like)
            postDao.decrementLikeCount(postId)
            false
        }
    }

    suspend fun likePost(userId: String, postId: String) {
        postDao.likePost(PostLikeEntity(userId = userId, postId = postId))
        postDao.incrementLikeCount(postId)
    }

    suspend fun unlikePost(userId: String, postId: String) {
        postDao.unlikePost(PostLikeEntity(userId = userId, postId = postId))
        postDao.decrementLikeCount(postId)
    }

    fun isPostLiked(userId: String, postId: String): Flow<Boolean> =
        postDao.isPostLiked(userId, postId)

    fun getLikedPosts(userId: String): Flow<List<PostEntity>> = postDao.getLikedPosts(userId)

    fun getLikedPostCount(userId: String): Flow<Int> = postDao.getLikedPostCount(userId)

    // Comment operations
    suspend fun addComment(
        postId: String,
        authorId: String,
        content: String,
        parentCommentId: String? = null
    ): CommentEntity {
        val comment = CommentEntity(
            commentId = UUID.randomUUID().toString(),
            postId = postId,
            authorId = authorId,
            content = content,
            parentCommentId = parentCommentId
        )
        postDao.insertComment(comment)
        postDao.incrementCommentCount(postId)
        return comment
    }

    fun getComments(postId: String): Flow<List<CommentEntity>> = postDao.getCommentsByPost(postId)

    fun observeCommentLikeAggregates(postId: String): Flow<List<CommentLikeAggregate>> =
        postDao.observeCommentLikeAggregates(postId)

    fun observeLikedCommentIdsForPost(userId: String, postId: String): Flow<List<String>> =
        postDao.observeLikedCommentIdsForPost(userId, postId)

    suspend fun toggleCommentLike(userId: String, commentId: String) {
        val like = CommentLikeEntity(userId = userId, commentId = commentId)
        if (postDao.isCommentLikedByUser(userId, commentId)) {
            postDao.deleteCommentLike(like)
        } else {
            postDao.insertCommentLike(like)
        }
    }

    /**
     * Deletes [commentId] and all nested replies. [deleterUserId] must be the comment author or the post author.
     * @return an error message, or null on success.
     */
    suspend fun deleteCommentThread(deleterUserId: String, commentId: String): String? {
        val comment = postDao.getCommentByIdOnce(commentId) ?: return "Comment not found"
        val post = postDao.getPostByIdOnce(comment.postId) ?: return "Post not found"
        val allowed = comment.authorId == deleterUserId || post.authorId == deleterUserId
        if (!allowed) return "You can't delete this comment"
        val all = postDao.getCommentsByPostOnce(comment.postId)
        val ids = commentSubtreeIds(all, commentId).toList()
        if (ids.isEmpty()) return null
        postDao.deleteCommentLikesForComments(ids)
        postDao.deleteCommentsByIds(ids)
        postDao.decrementCommentCountBy(comment.postId, ids.size)
        return null
    }

    private fun commentSubtreeIds(allComments: List<CommentEntity>, rootId: String): Set<String> {
        val byParent = allComments.groupBy { it.parentCommentId }
        val out = linkedSetOf<String>()
        fun walk(id: String) {
            if (!out.add(id)) return
            byParent[id]?.forEach { walk(it.commentId) }
        }
        walk(rootId)
        return out
    }

    fun getPostCount(userId: String): Flow<Int> = postDao.getPostCount(userId)
}
