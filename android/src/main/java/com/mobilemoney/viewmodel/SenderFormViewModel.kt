package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.data.local.SenderEntity
import com.mobilemoney.data.local.SenderType
import com.mobilemoney.di.DI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.common.FormField
import java.util.UUID

data class SenderFormState(
    val senderNumber: FormField = FormField(label = "Идентификатор отправителя"),
    val label: FormField = FormField(label = "Метка"),
    val senderType: SenderType = SenderType.PHONE_NUMBER,
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
                    senderNumber = _uiState.value.senderNumber.withValue(sender.sender),
                    label = _uiState.value.label.withValue(sender.label),
                    senderType = SenderType.valueOf(sender.type),
                    isEditing = true,
                    senderId = id
                )
            }
        }
    }

    fun updateSenderNumber(value: String) {
        _uiState.value = _uiState.value.copy(
            senderNumber = _uiState.value.senderNumber.withValue(value)
        )
    }

    fun updateLabel(value: String) {
        _uiState.value = _uiState.value.copy(
            label = _uiState.value.label.withValue(value)
        )
    }

    fun updateSenderType(type: SenderType) {
        _uiState.value = _uiState.value.copy(senderType = type)
    }

    fun save() {
        val state = _uiState.value
        var hasError = false

        val cleanNumber = state.senderNumber.validate()
        if (!cleanNumber.isValid) {
            _uiState.value = state.copy(senderNumber = cleanNumber)
            hasError = true
        }

        val cleanLabel = state.label.validate()
        if (!cleanLabel.isValid) {
            _uiState.value = state.copy(label = cleanLabel)
            hasError = true
        }

        if (hasError) {
            viewModelScope.launch {
                ErrorHandler.emitError("Заполните обязательные поля")
            }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (state.isEditing && state.senderId != null) {
                DI.databaseRepository.updateSender(
                    SenderEntity(
                        id = state.senderId,
                        sender = state.senderNumber.value,
                        label = state.label.value,
                        type = state.senderType.name,
                        createdAt = 0,
                        updatedAt = now
                    )
                )
            } else {
                DI.databaseRepository.addSender(
                    SenderEntity(
                        id = UUID.randomUUID().toString(),
                        sender = state.senderNumber.value,
                        label = state.label.value,
                        type = state.senderType.name,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun deleteSender() {
        val id = _uiState.value.senderId ?: return
        viewModelScope.launch {
            DI.databaseRepository.deleteSender(id)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun resetState() {
        _uiState.value = SenderFormState()
    }
}
