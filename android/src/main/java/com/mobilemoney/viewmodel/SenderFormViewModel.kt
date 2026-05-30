package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.SenderType
import com.mobilemoney.domain.repository.SenderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.common.FormField

data class SenderFormState(
    val senderNumber: FormField = FormField(label = "Идентификатор отправителя"),
    val label: FormField = FormField(label = "Метка"),
    val senderType: SenderType = SenderType.PHONE_NUMBER,
    val isEditing: Boolean = false,
    val senderId: String? = null,
    val isSaved: Boolean = false
)

class SenderFormViewModel(
    private val senderRepository: SenderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SenderFormState())
    val uiState: StateFlow<SenderFormState> = _uiState.asStateFlow()

    fun loadSender(id: String) {
        viewModelScope.launch {
            val sender = senderRepository.getSenderById(id)
            if (sender != null) {
                _uiState.value = _uiState.value.copy(
                    senderNumber = _uiState.value.senderNumber.withValue(sender.sender),
                    label = _uiState.value.label.withValue(sender.label),
                    senderType = sender.type,
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
            if (state.isEditing && state.senderId != null) {
                senderRepository.updateSender(
                    id = state.senderId,
                    sender = state.senderNumber.value,
                    label = state.label.value,
                    type = state.senderType
                )
            } else {
                senderRepository.addSender(
                    sender = state.senderNumber.value,
                    label = state.label.value,
                    type = state.senderType
                )
            }
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun deleteSender() {
        val id = _uiState.value.senderId ?: return
        viewModelScope.launch {
            senderRepository.deleteSender(id)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun resetState() {
        _uiState.value = SenderFormState()
    }
}
