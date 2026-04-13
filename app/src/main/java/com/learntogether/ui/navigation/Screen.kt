package com.learntogether.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class defining all navigation routes in the app.
 */
sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Main tabs
    data object Feed : Screen("feed")
    data object Courses : Screen("courses")
    data object Search : Screen("search")
    data object Profile : Screen("profile")

    // Detail screens
    data object PostDetail : Screen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }
    data object CreatePost : Screen("create_post")
    data object EditPost : Screen("edit_post/{postId}") {
        fun createRoute(postId: String) = "edit_post/$postId"
    }
    data object CourseDetail : Screen("course/{courseId}") {
        fun createRoute(courseId: String) = "course/$courseId"
    }
    data object CreateCourse : Screen("create_course")
    data object EditCourse : Screen("edit_course/{courseId}") {
        fun createRoute(courseId: String) = "edit_course/$courseId"
    }
    data object ChapterDetail : Screen("chapter/{chapterId}") {
        fun createRoute(chapterId: String) = "chapter/$chapterId"
    }
    data object EditChapter : Screen("edit_chapter/{chapterId}") {
        fun createRoute(chapterId: String) = "edit_chapter/$chapterId"
    }
    data object ChapterChat : Screen("chapter_chat/{chapterId}") {
        fun createRoute(chapterId: String) = "chapter_chat/$chapterId"
    }
    data object UserProfile : Screen("user/{userId}") {
        fun createRoute(userId: String) = "user/$userId"
    }

    data object EditProfile : Screen("edit_profile")

    // Tools
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
    data object Pomodoro : Screen("pomodoro")
    data object Challenges : Screen("challenges")
}

/**
 * Defines the bottom navigation items.
 */
enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    FEED(
        screen = Screen.Feed,
        label = "Feed",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    COURSES(
        screen = Screen.Courses,
        label = "Courses",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    SEARCH(
        screen = Screen.Search,
        label = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    PROFILE(
        screen = Screen.Profile,
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}
