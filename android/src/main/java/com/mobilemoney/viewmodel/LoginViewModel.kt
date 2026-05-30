package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mobilemoney.ui.common.ErrorHandler

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val serverStatus: ServerStatus = ServerStatus.UNKNOWN
)

enum class ServerStatus {
    UNKNOWN,
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

class LoginViewModel(
    private val syncRepository: SyncRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(login: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = syncRepository.login(login, password)

            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, isLoggedIn = true)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Ошибка входа"
                ErrorHandler.emitError(errorMsg)
                _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun checkServerConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(serverStatus = ServerStatus.CHECKING)

            android.util.Log.d("LoginVM", "Server URL: ${syncRepository.serverUrl}")
            val result = syncRepository.ping()

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(serverStatus = ServerStatus.AVAILABLE)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Сервер недоступен"
                ErrorHandler.emitError(errorMsg)
                _uiState.value = _uiState.value.copy(serverStatus = ServerStatus.UNAVAILABLE)
            }
        }
    }
}