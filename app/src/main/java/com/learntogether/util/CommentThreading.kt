package com.learntogether.util

import com.learntogether.data.local.entity.CommentEntity

/**
 * Groups flat comments into (root, replies) where replies are all non-root comments in the same thread,
 * ordered by [CommentEntity.createdAt]. Root is the top-level comment ([parentCommentId] null).
 */
fun buildCommentThreads(comments: List<CommentEntity>): List<Pair<CommentEntity, List<CommentEntity>>> {
    if (comments.isEmpty()) return emptyList()
    val byId = comments.associateBy { it.commentId }

    fun rootOf(c: CommentEntity): CommentEntity {
        var x = c
        while (x.parentCommentId != null) {
            x = byId[x.parentCommentId!!] ?: break
        }
        return x
    }

    val grouped = comments.groupBy { rootOf(it).commentId }
    return grouped.mapNotNull { (_, group) ->
        val root = group.find { it.parentCommentId == null } ?: return@mapNotNull null
        val replies = group.filter { it.commentId != root.commentId }.sortedBy { it.createdAt }
        root to replies
    }.sortedBy { it.first.createdAt }
}
