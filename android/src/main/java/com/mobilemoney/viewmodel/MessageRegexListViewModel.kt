package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.MessageRegex
import com.mobilemoney.domain.usecase.clipboard.GetMessageRegexesUseCase
import com.mobilemoney.domain.usecase.clipboard.DeleteMessageRegexUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class MessageRegexListState(
    val regexes: List<MessageRegex> = emptyList(),
    val isLoading: Boolean = false
)

class MessageRegexListViewModel(
    private val getRegexesUseCase: GetMessageRegexesUseCase,
    private val deleteRegexUseCase: DeleteMessageRegexUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageRegexListState())
    val uiState: StateFlow<MessageRegexListState> = _uiState.asStateFlow()

    init {
        loadRegexes()
    }

    private fun loadRegexes() {
        viewModelScope.launch {
            getRegexesUseCase().collect { regexes ->
                _uiState.value = _uiState.value.copy(regexes = regexes)
            }
        }
    }

    fun deleteRegex(id: UUID) {
        viewModelScope.launch {
            deleteRegexUseCase(id)
        }
    }
}
