package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val serverStatus: ServerStatus = ServerStatus.UNKNOWN
)

enum class ServerStatus {
    UNKNOWN,
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(login: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val app = com.mobilemoney.MobileMoneyApp.instance
            val syncRepository = app.syncRepository

            val result = syncRepository.login(login, password)

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, isLoggedIn = true)
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Ошибка входа"
                )
            }
        }
    }

    fun checkServerConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(serverStatus = ServerStatus.CHECKING)

            val app = com.mobilemoney.MobileMoneyApp.instance
            val syncRepository = app.syncRepository

            android.util.Log.d("LoginVM", "Server URL: ${syncRepository.serverUrl}")
            val result = syncRepository.ping()

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(serverStatus = ServerStatus.AVAILABLE)
            } else {
                val error = result.exceptionOrNull()
                _uiState.value.copy(serverStatus = ServerStatus.UNAVAILABLE, error = error?.message)
            }
        }
    }
}