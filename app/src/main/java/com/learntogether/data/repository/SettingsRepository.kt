package com.learntogether.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for user settings/preferences using DataStore.
 * Manages dark mode, font style, and other app preferences.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val POMODORO_WORK_MINUTES = intPreferencesKey("pomodoro_work_minutes")
        val POMODORO_BREAK_MINUTES = intPreferencesKey("pomodoro_break_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val fontStyle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.FONT_STYLE] ?: "Default"
    }

    val accentPaletteKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_PALETTE] ?: "teal"
    }

    val pomodoroWorkMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.POMODORO_WORK_MINUTES] ?: 30
    }

    val pomodoroBreakMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.POMODORO_BREAK_MINUTES] ?: 5
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setFontStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_STYLE] = style
        }
    }

    suspend fun setAccentPaletteKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCENT_PALETTE] = key
        }
    }

    suspend fun setPomodoroWorkMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POMODORO_WORK_MINUTES] = minutes
        }
    }

    suspend fun setPomodoroBreakMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POMODORO_BREAK_MINUTES] = minutes
        }
    }
}
