package com.learntogether.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Single @-prefix for UI; stored handles may already include `@`.
 */
fun displayHandle(handle: String): String {
    val h = handle.trim().removePrefix("@").trim()
    return if (h.isEmpty()) "@" else "@$h"
}

/**
 * Utility functions used across the app.
 */
object TimeUtils {
    /**
     * Converts a timestamp to a relative time string (e.g., "2h ago", "3d ago").
     */
    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff) / 7}w ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    /**
     * Formats minutes into hours and minutes string.
     */
    fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    /**
     * Formats a timestamp to a readable date string.
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Returns remaining days until a deadline.
     */
    fun getDaysRemaining(deadlineTimestamp: Long): Int {
        val diff = deadlineTimestamp - System.currentTimeMillis()
        return if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff).toInt() else 0
    }
}

/**
 * Course categories available in the app.
 */
object Categories {
    val all = listOf(
        "Programming",
        "Data Science",
        "Mathematics",
        "Physics",
        "Chemistry",
        "Biology",
        "History",
        "Literature",
        "Languages",
        "Business",
        "Design",
        "Music",
        "Art",
        "Psychology",
        "Philosophy",
        "Engineering",
        "Medicine",
        "Law",
        "Economics",
        "Other"
    )
}
