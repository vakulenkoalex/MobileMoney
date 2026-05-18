package com.mobilemoney.domain.usecase.category

import com.mobilemoney.domain.model.Category
import com.mobilemoney.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return categoryRepository.getCategories()
    }
}