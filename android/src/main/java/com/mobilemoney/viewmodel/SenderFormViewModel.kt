package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.data.local.SenderEntity
import com.mobilemoney.di.DI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SenderFormState(
    val senderNumber: String = "",
    val label: String = "",
    val isEditing: Boolean = false,
    val senderId: String? = null,
    val isSaved: Boolean = false
)

class SenderFormViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SenderFormState())
    val uiState: StateFlow<SenderFormState> = _uiState.asStateFlow()

    fun loadSender(id: String) {
        viewModelScope.launch {
            val sender = DI.databaseRepository.getSenderById(id)
            if (sender != null) {
                _uiState.value = _uiState.value.copy(
                    senderNumber = sender.sender,
                    label = sender.label ?: "",
                    isEditing = true,
                    senderId = id
                )
            }
        }
    }

    fun updateSenderNumber(value: String) {
        _uiState.value = _uiState.value.copy(senderNumber = value)
    }

    fun updateLabel(value: String) {
        _uiState.value = _uiState.value.copy(label = value)
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (state.isEditing && state.senderId != null) {
                DI.databaseRepository.updateSender(
                    SenderEntity(
                        id = state.senderId,
                        sender = state.senderNumber,
                        label = state.label.takeIf { it.isNotBlank() },
                        createdAt = 0,
                        updatedAt = now
                    )
                )
            } else {
                DI.databaseRepository.addSender(
                    SenderEntity(
                        id = UUID.randomUUID().toString(),
                        sender = state.senderNumber,
                        label = state.label.takeIf { it.isNotBlank() },
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun resetState() {
        _uiState.value = SenderFormState()
    }
}
