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
import java.util.UUID

data class MessageRegexFormState(
    val label: String = "",
    val labelError: String? = null,
    val pattern: String = "",
    val skipBalanceCheck: Boolean = false,
    val patternError: String? = null,
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
                        label = regex.label,
                        pattern = regex.pattern,
                        skipBalanceCheck = regex.skipBalanceCheck,
                        isEditing = true,
                        regexId = id
                    )
                }
            }
        }
    }

    fun updateLabel(label: String) {
        _uiState.value = _uiState.value.copy(label = label, labelError = null)
    }

    fun updatePattern(pattern: String) {
        _uiState.value = _uiState.value.copy(pattern = pattern, patternError = null)
    }

    fun updateSkipBalanceCheck(skip: Boolean) {
        _uiState.value = _uiState.value.copy(skipBalanceCheck = skip)
    }

    fun validateAndSave(): Boolean {
        val state = _uiState.value
        if (state.label.isBlank()) {
            _uiState.value = state.copy(labelError = "Введите название")
            return false
        }
        if (state.pattern.isBlank()) {
            _uiState.value = state.copy(patternError = "Введите regex")
            return false
        }
        if (!state.skipBalanceCheck && !state.pattern.contains("(?<balance>")) {
            _uiState.value = state.copy(
                patternError = "Regex должен содержать (?<balance>...), если не установлена галочка"
            )
            return false
        }
        return try {
            Regex(state.pattern)
            val namedGroupRegex = Regex("\\(\\?<([a-zA-Z_]+)>")
            val groupNames = namedGroupRegex.findAll(state.pattern)
                .map { it.groupValues[1] }
                .toSet()
            val requiredGroups = listOf("amount", "shop", "cardMask", "direction")
            val allPresent = requiredGroups.all { it in groupNames }
            if (!allPresent) {
                _uiState.value = _uiState.value.copy(
                    patternError = "Regex должен содержать именованные группы: amount, shop, cardMask, direction"
                )
                return false
            }
            save()
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(patternError = "Неверный regex: ${e.message}")
            false
        }
    }

    private fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            saveRegexUseCase(
                MessageRegex(
                    id = state.regexId ?: UUID.randomUUID(),
                    label = state.label,
                    pattern = state.pattern,
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
