package com.mobilemoney.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.data.repository.BackupRepository
import com.mobilemoney.di.DI
import com.mobilemoney.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false,
    val isSyncing: Boolean = false
)

class SettingsViewModel(
    private val syncRepository: SyncRepository = DI.syncRepository,
    private val backupRepository: BackupRepository = BackupRepository(DI.context)
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun export(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }

            backupRepository.export(uri)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(isLoading = false, message = message, isSuccess = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message, isSuccess = false)
                    }
                }
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }

            backupRepository.import(uri)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(isLoading = false, message = message, isSuccess = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message, isSuccess = false)
                    }
                }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                DI.databaseRepository.permanentlyDeleteAll()
                _uiState.update {
                    it.copy(isLoading = false, message = "Удалённые записи удалены", isSuccess = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, message = e.message, isSuccess = false)
                }
            }
        }
    }

    fun getDefaultFileName(): String = backupRepository.generateFileName()

    fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSyncing = true, message = null) }

            val result = syncRepository.sync()
            result.onSuccess {
                _uiState.update {
                    it.copy(isLoading = false, isSyncing = false, message = "Синхронизация завершена", isSuccess = true)
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isLoading = false, isSyncing = false, message = null, isSuccess = false)
                }
            }
        }
    }
}