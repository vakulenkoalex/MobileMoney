package com.mobilemoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryListState(
    val categories: List<CategoryUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CategoryListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)

    private val _uiState = MutableStateFlow(CategoryListState())
    val uiState: StateFlow<CategoryListState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    isLoading = false
                )
            }
        }
    }
}