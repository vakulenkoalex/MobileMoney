package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.Category
import com.mobilemoney.domain.usecase.category.GetCategoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryListState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CategoryListViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryListState())
    val uiState: StateFlow<CategoryListState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getCategoriesUseCase().collect { categories ->
                val sorted = categories.sortedBy { it.name }
                val roots = sorted.filter { it.parentId == null }
                val grouped = mutableListOf<Category>()
                for (root in roots) {
                    grouped.add(root)
                    val children = sorted.filter { it.parentId == root.id }
                    grouped.addAll(children)
                }
                _uiState.value = _uiState.value.copy(
                    categories = grouped,
                    isLoading = false
                )
            }
        }
    }
}
