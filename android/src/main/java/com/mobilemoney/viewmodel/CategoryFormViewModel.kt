package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.data.config.CategoryIconOption
import com.mobilemoney.data.config.CategoryIcons
import com.mobilemoney.di.DI
import com.mobilemoney.domain.model.Category
import com.mobilemoney.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import com.mobilemoney.ui.common.ErrorHandler
import java.util.UUID

data class CategoryFormState(
    val name: String = "",
    val icon: String = "restaurant",
    val isIncome: Boolean = false,
    val isDefault: Boolean = false,
    val parentId: UUID? = null,
    val isEditing: Boolean = false,
    val categoryId: UUID? = null,
    val icons: List<CategoryIconOption> = CategoryIcons.all,
    val parentCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class CategoryFormViewModel(
    private val categoryRepository: CategoryRepository = DI.categoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryFormState())
    val uiState: StateFlow<CategoryFormState> = _uiState.asStateFlow()

    init {
        loadParentCategories()
    }

    private fun loadParentCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories().collect { categories ->
                val filtered = categories
                    .filter { it.parentId == null && it.isIncome == _uiState.value.isIncome }
                    .sortedBy { it.name }
                _uiState.value = _uiState.value.copy(parentCategories = filtered)
            }
        }
    }

    fun loadCategory(categoryId: UUID) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val categories = categoryRepository.getCategories().first()
            val category = categories.find { it.id == categoryId }
            if (category != null) {
                _uiState.value = _uiState.value.copy(
                    name = category.name,
                    icon = category.icon,
                    isIncome = category.isIncome,
                    isDefault = category.isDefault,
                    parentId = category.parentId,
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
        val filtered = _uiState.value.parentCategories
            .filter { it.isIncome == isIncome }
        _uiState.value = _uiState.value.copy(parentCategories = filtered)
    }

    fun updateIsDefault(isDefault: Boolean) {
        _uiState.value = _uiState.value.copy(isDefault = isDefault)
    }

    fun updateParentId(parentId: UUID?) {
        _uiState.value = _uiState.value.copy(parentId = parentId)
    }

    fun resetState() {
        _uiState.value = CategoryFormState()
        loadParentCategories()
    }

    fun save(): Boolean {
        val state = _uiState.value

        if (state.name.isBlank()) {
            GlobalScope.launch {
                ErrorHandler.emitError("Введите название категории")
            }
            return false
        }

        val category = Category(
            id = state.categoryId ?: UUID.randomUUID(),
            name = state.name,
            icon = state.icon,
            isIncome = state.isIncome,
            isDefault = state.isDefault,
            parentId = state.parentId
        )

        viewModelScope.launch {
            if (state.isDefault) {
                categoryRepository.clearDefaultCategories(state.isIncome)
            }
            if (state.isEditing) {
                categoryRepository.updateCategory(category)
            } else {
                categoryRepository.addCategory(category)
            }
        }

        _uiState.value = state.copy(isSaved = true)
        return true
    }
}
