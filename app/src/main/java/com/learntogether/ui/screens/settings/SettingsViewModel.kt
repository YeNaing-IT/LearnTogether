package com.learntogether.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.local.entity.UserEntity
import com.learntogether.data.repository.SettingsRepository
import com.learntogether.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AccountDialog {
    None,
    ChangeEmail,
    ChangeUsername,
    ChangeHandle,
    DeleteAccount
}

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val fontStyle: String = "Default",
    val accentPaletteKey: String = "teal",
    val showUpdateDialog: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val currentUser: UserEntity? = null,
    val accountDialog: AccountDialog = AccountDialog.None,
    val accountActionInProgress: Boolean = false,
    val accountFormError: String? = null,
    val accountSuccessMessage: String? = null,
    val wipeAllDataError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.isDarkMode.collect { v -> _uiState.update { it.copy(isDarkMode = v) } }
        }
        viewModelScope.launch {
            settingsRepository.fontStyle.collect { v -> _uiState.update { it.copy(fontStyle = v) } }
        }
        viewModelScope.launch {
            settingsRepository.accentPaletteKey.collect { key ->
                _uiState.update { it.copy(accentPaletteKey = key) }
            }
        }
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user -> _uiState.update { it.copy(currentUser = user) } }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch { settingsRepository.setDarkMode(!_uiState.value.isDarkMode) }
    }

    fun setFontStyle(style: String) {
        viewModelScope.launch { settingsRepository.setFontStyle(style) }
    }

    fun setAccentPalette(key: String) {
        viewModelScope.launch { settingsRepository.setAccentPaletteKey(key) }
    }

    fun showUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = true) }
    }

    fun hideUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun hideLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun logout() {
        viewModelScope.launch { userRepository.logout() }
    }

    fun openAccountDialog(kind: AccountDialog) {
        _uiState.update { it.copy(accountDialog = kind, accountFormError = null) }
    }

    fun dismissAccountDialog() {
        _uiState.update { it.copy(accountDialog = AccountDialog.None, accountFormError = null) }
    }

    fun consumeAccountSuccessMessage() {
        _uiState.update { it.copy(accountSuccessMessage = null) }
    }

    fun consumeWipeAllDataError() {
        _uiState.update { it.copy(wipeAllDataError = null) }
    }

    fun wipeAllLocalData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountActionInProgress = true, wipeAllDataError = null) }
            val result = userRepository.wipeAllLocalDatabaseData()
            if (result.isSuccess) {
                _uiState.update { it.copy(accountActionInProgress = false, currentUser = null) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        accountActionInProgress = false,
                        wipeAllDataError = result.exceptionOrNull()?.message ?: "Could not clear database"
                    )
                }
            }
        }
    }

    fun submitEmailChange(newEmail: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountActionInProgress = true, accountFormError = null) }
            val result = userRepository.updateCurrentUserEmail(newEmail, password)
            _uiState.update { s ->
                if (result.isSuccess) {
                    s.copy(
                        accountActionInProgress = false,
                        accountDialog = AccountDialog.None,
                        accountSuccessMessage = "Email updated"
                    )
                } else {
                    s.copy(
                        accountActionInProgress = false,
                        accountFormError = result.exceptionOrNull()?.message ?: "Could not update email"
                    )
                }
            }
        }
    }

    fun submitUsernameChange(newUsername: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountActionInProgress = true, accountFormError = null) }
            val result = userRepository.updateCurrentUserUsername(newUsername, password)
            _uiState.update { s ->
                if (result.isSuccess) {
                    s.copy(
                        accountActionInProgress = false,
                        accountDialog = AccountDialog.None,
                        accountSuccessMessage = "Display name updated"
                    )
                } else {
                    s.copy(
                        accountActionInProgress = false,
                        accountFormError = result.exceptionOrNull()?.message ?: "Could not update name"
                    )
                }
            }
        }
    }

    fun submitHandleChange(newHandle: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountActionInProgress = true, accountFormError = null) }
            val result = userRepository.updateCurrentUserHandle(newHandle, password)
            _uiState.update { s ->
                if (result.isSuccess) {
                    s.copy(
                        accountActionInProgress = false,
                        accountDialog = AccountDialog.None,
                        accountSuccessMessage = "Handle updated"
                    )
                } else {
                    s.copy(
                        accountActionInProgress = false,
                        accountFormError = result.exceptionOrNull()?.message ?: "Could not update handle"
                    )
                }
            }
        }
    }

    fun submitDeleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountActionInProgress = true, accountFormError = null) }
            val result = userRepository.deleteCurrentUserAccount(password)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        accountActionInProgress = false,
                        accountDialog = AccountDialog.None,
                        currentUser = null
                    )
                }
                onDeleted()
            } else {
                _uiState.update {
                    it.copy(
                        accountActionInProgress = false,
                        accountFormError = result.exceptionOrNull()?.message ?: "Could not delete account"
                    )
                }
            }
        }
    }
}
