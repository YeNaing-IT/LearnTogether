package com.learntogether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.learntogether.data.repository.SettingsRepository
import com.learntogether.data.repository.UserRepository
import com.learntogether.ui.navigation.BottomNavItem
import com.learntogether.ui.navigation.NavGraph
import com.learntogether.ui.navigation.Screen
import com.learntogether.ui.theme.AppAccentPalette
import com.learntogether.ui.theme.AppFontStyle
import com.learntogether.ui.theme.LearnTogetherTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LearnTogetherAppContent()
        }
    }
}

data class MainUiState(
    val isDarkMode: Boolean = false,
    val fontStyle: AppFontStyle = AppFontStyle.DEFAULT,
    val accentPaletteKey: String = "teal",
    val isLoggedIn: Boolean? = null  // null = loading
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.isDarkMode.collect { dark ->
                _uiState.update { it.copy(isDarkMode = dark) }
            }
        }
        viewModelScope.launch {
            settingsRepository.fontStyle.collect { name ->
                _uiState.update { it.copy(fontStyle = AppFontStyle.fromName(name)) }
            }
        }
        viewModelScope.launch {
            settingsRepository.accentPaletteKey.collect { key ->
                _uiState.update { it.copy(accentPaletteKey = key) }
            }
        }
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(isLoggedIn = user != null) }
            }
        }
    }
}

@Composable
fun LearnTogetherAppContent(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    // Determine start destination
    val startDestination = when (uiState.isLoggedIn) {
        null -> return  // Still loading
        true -> Screen.Feed.route
        false -> Screen.Login.route
    }

    LearnTogetherTheme(
        darkTheme = uiState.isDarkMode,
        fontStyle = uiState.fontStyle,
        accentPalette = AppAccentPalette.fromKey(uiState.accentPaletteKey)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Show bottom nav only on main tabs
        val mainTabRoutes = BottomNavItem.entries.map { it.screen.route }
        val showBottomBar = currentRoute in mainTabRoutes

        Scaffold(
            // Avoid double top inset: child screens' TopAppBars already handle the status bar.
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            ),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        BottomNavItem.entries.forEach { item ->
                            val selected = navBackStackEntry?.destination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Feed.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
