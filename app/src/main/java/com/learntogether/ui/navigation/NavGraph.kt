package com.learntogether.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.learntogether.ui.screens.auth.LoginScreen
import com.learntogether.ui.screens.auth.RegisterScreen
import com.learntogether.ui.screens.feed.FeedScreen
import com.learntogether.ui.screens.course.*
import com.learntogether.ui.screens.post.*
import com.learntogether.ui.screens.profile.ProfileScreen
import com.learntogether.ui.screens.profile.EditProfileScreen
import com.learntogether.ui.screens.profile.UserProfileScreen
import com.learntogether.ui.screens.search.SearchScreen
import com.learntogether.ui.screens.chat.ChapterChatScreen
import com.learntogether.ui.screens.settings.SettingsScreen
import com.learntogether.ui.screens.statistics.StatisticsScreen
import com.learntogether.ui.screens.pomodoro.PomodoroScreen
import com.learntogether.ui.screens.challenges.ChallengesScreen

/**
 * Main navigation graph for the entire application.
 * Uses Jetpack Navigation Compose for type-safe navigation.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        //  Auth
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        //  Main Tabs
        composable(Screen.Feed.route) {
            FeedScreen(
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onCreatePost = { navController.navigate(Screen.CreatePost.route) },
                onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onNavigateToPomodoro = { navController.navigate(Screen.Pomodoro.route) },
                onNavigateToChallenges = { navController.navigate(Screen.Challenges.route) },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) }
            )
        }

        composable(Screen.Courses.route) {
            CoursesScreen(
                onCourseClick = { courseId -> navController.navigate(Screen.CourseDetail.createRoute(courseId)) },
                onCreateCourse = { navController.navigate(Screen.CreateCourse.route) }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onCourseClick = { courseId -> navController.navigate(Screen.CourseDetail.createRoute(courseId)) },
                onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onCourseClick = { courseId -> navController.navigate(Screen.CourseDetail.createRoute(courseId)) },
                onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onStatisticsClick = { navController.navigate(Screen.Statistics.route) },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(onBack = { navController.popBackStack() })
        }

        //  Detail Screens
        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            PostDetailScreen(
                postId = postId,
                onBack = { navController.popBackStack() },
                onUserClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onEditPost = { pid -> navController.navigate(Screen.EditPost.createRoute(pid)) },
                onPostDeleted = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditPost.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) {
            EditPostScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onBack = { navController.popBackStack() },
                onPostCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CourseDetail.route,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
            CourseDetailScreen(
                courseId = courseId,
                onBack = { navController.popBackStack() },
                onChapterClick = { chapterId -> navController.navigate(Screen.ChapterDetail.createRoute(chapterId)) },
                onChapterChatClick = { chapterId -> navController.navigate(Screen.ChapterChat.createRoute(chapterId)) },
                onEditCourse = { cid -> navController.navigate(Screen.EditCourse.createRoute(cid)) },
                onCourseDeleted = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditCourse.route,
            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
        ) {
            EditCourseScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateCourse.route) {
            CreateCourseScreen(
                onBack = { navController.popBackStack() },
                onCourseCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ChapterDetail.route,
            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
            ChapterDetailScreen(
                chapterId = chapterId,
                onBack = { navController.popBackStack() },
                onChatClick = { navController.navigate(Screen.ChapterChat.createRoute(chapterId)) },
                onEditChapter = { cid -> navController.navigate(Screen.EditChapter.createRoute(cid)) }
            )
        }

        composable(
            route = Screen.EditChapter.route,
            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
        ) {
            EditChapterScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ChapterChat.route,
            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
            ChapterChatScreen(
                chapterId = chapterId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onCourseClick = { courseId -> navController.navigate(Screen.CourseDetail.createRoute(courseId)) },
                onEditProfile = { navController.navigate(Screen.EditProfile.route) }
            )
        }

        //  Tools
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Pomodoro.route) {
            PomodoroScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Challenges.route) {
            ChallengesScreen(onBack = { navController.popBackStack() })
        }
    }
}
