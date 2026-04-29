package com.mobilemoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.data.config.CategoryIconOption
import com.mobilemoney.data.config.CategoryIcons
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoryFormState(
    val name: String = "",
    val icon: String = "restaurant",
    val isIncome: Boolean = false,
    val isEditing: Boolean = false,
    val categoryId: UUID? = null,
    val icons: List<CategoryIconOption> = CategoryIcons.all,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class CategoryFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)

    private val _uiState = MutableStateFlow(CategoryFormState())
    val uiState: StateFlow<CategoryFormState> = _uiState.asStateFlow()

    fun loadCategory(categoryId: UUID) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val categories = repository.getCategories().first()
            val category = categories.find { it.id == categoryId }
            if (category != null) {
                _uiState.value = _uiState.value.copy(
                    name = category.name,
                    icon = category.icon,
                    isIncome = category.isIncome,
                    isEditing = true,
                    categoryId = categoryId,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Категория не найдена"
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateIcon(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun updateIsIncome(isIncome: Boolean) {
        _uiState.value = _uiState.value.copy(isIncome = isIncome)
    }

    fun save(): Boolean {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Введите название категории")
            return false
        }

        val category = CategoryUi(
            id = state.categoryId ?: UUID.randomUUID(),
            name = state.name,
            icon = state.icon,
            isIncome = state.isIncome
        )

        viewModelScope.launch {
            if (state.isEditing) {
                repository.updateCategory(category)
            } else {
                repository.addCategory(category)
            }
        }

        _uiState.value = state.copy(isSaved = true)
        return true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}