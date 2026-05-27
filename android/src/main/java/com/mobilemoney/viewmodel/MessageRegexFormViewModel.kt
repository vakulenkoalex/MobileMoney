package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.MessageRegex
import com.mobilemoney.domain.usecase.clipboard.GetMessageRegexesUseCase
import com.mobilemoney.domain.usecase.clipboard.SaveMessageRegexUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.common.FormField
import java.util.UUID

data class MessageRegexFormState(
    val label: FormField = FormField(label = "Название (метка)"),
    val pattern: FormField = FormField(label = "Regex pattern"),
    val skipBalanceCheck: Boolean = false,
    val isEditing: Boolean = false,
    val regexId: UUID? = null,
    val isSaved: Boolean = false
)

class MessageRegexFormViewModel(
    private val getRegexesUseCase: GetMessageRegexesUseCase,
    private val saveRegexUseCase: SaveMessageRegexUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageRegexFormState())
    val uiState: StateFlow<MessageRegexFormState> = _uiState.asStateFlow()

    fun loadRegex(id: UUID) {
        viewModelScope.launch {
            getRegexesUseCase().collect { regexes ->
                val regex = regexes.find { it.id == id }
                if (regex != null) {
                    _uiState.value = _uiState.value.copy(
                        label = _uiState.value.label.withValue(regex.label),
                        pattern = _uiState.value.pattern.withValue(regex.pattern),
                        skipBalanceCheck = regex.skipBalanceCheck,
                        isEditing = true,
                        regexId = id
                    )
                }
            }
        }
    }

    fun updateLabel(label: String) {
        _uiState.value = _uiState.value.copy(
            label = _uiState.value.label.withValue(label)
        )
    }

    fun updatePattern(pattern: String) {
        _uiState.value = _uiState.value.copy(
            pattern = _uiState.value.pattern.withValue(pattern)
        )
    }

    fun updateSkipBalanceCheck(skip: Boolean) {
        _uiState.value = _uiState.value.copy(skipBalanceCheck = skip)
    }

    fun validateAndSave(): Boolean {
        val state = _uiState.value

        val cleanLabel = state.label.validate()
        if (!cleanLabel.isValid) {
            _uiState.value = state.copy(label = cleanLabel)
            viewModelScope.launch {
                ErrorHandler.emitError("Заполните обязательные поля")
            }
            return false
        }

        val cleanPattern = state.pattern.validate()
        if (!cleanPattern.isValid) {
            _uiState.value = state.copy(pattern = cleanPattern)
            viewModelScope.launch {
                ErrorHandler.emitError("Заполните обязательные поля")
            }
            return false
        }

        if (!state.skipBalanceCheck && !state.pattern.value.contains("(?<balance>")) {
            _uiState.value = state.copy(
                pattern = state.pattern.copy(error = "Regex должен содержать (?<balance>...), если не установлена галочка")
            )
            viewModelScope.launch {
                ErrorHandler.emitError("Заполните обязательные поля")
            }
            return false
        }

        return try {
            Regex(state.pattern.value)
            val namedGroupRegex = Regex("\\(\\?<([a-zA-Z_]+)>")
            val groupNames = namedGroupRegex.findAll(state.pattern.value)
                .map { it.groupValues[1] }
                .toSet()
            val requiredGroups = listOf("amount", "shop", "cardMask", "direction")
            val allPresent = requiredGroups.all { it in groupNames }
            if (!allPresent) {
                _uiState.value = _uiState.value.copy(
                    pattern = state.pattern.copy(error = "Regex должен содержать именованные группы: amount, shop, cardMask, direction")
                )
                viewModelScope.launch {
                    ErrorHandler.emitError("Regex должен содержать все обязательные группы")
                }
                return false
            }
            save()
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                pattern = state.pattern.copy(error = "Неверный regex: ${e.message}")
            )
            viewModelScope.launch {
                ErrorHandler.emitError("Неверный regex")
            }
            false
        }
    }

    private fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            saveRegexUseCase(
                MessageRegex(
                    id = state.regexId ?: UUID.randomUUID(),
                    label = state.label.value,
                    pattern = state.pattern.value,
                    skipBalanceCheck = state.skipBalanceCheck
                )
            )
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun resetState() {
        _uiState.value = MessageRegexFormState()
    }
}
