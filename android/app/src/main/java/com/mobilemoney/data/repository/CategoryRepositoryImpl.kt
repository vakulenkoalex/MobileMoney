package com.mobilemoney.data.repository

import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.domain.model.Category
import com.mobilemoney.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val databaseRepository: DatabaseRepository
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> {
        return databaseRepository.getCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return getCategories().first().find { it.id.toString() == id }
    }

    override suspend fun addCategory(category: Category) {
        databaseRepository.addCategory(category.toUiModel())
    }

    override suspend fun updateCategory(category: Category) {
        databaseRepository.updateCategory(category.toUiModel())
    }

    override suspend fun deleteCategory(id: String) {
        databaseRepository.deleteCategory(id)
    }
}

private fun CategoryUi.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        isIncome = isIncome
    )
}

private fun Category.toUiModel(): CategoryUi {
    return CategoryUi(
        id = id,
        name = name,
        icon = icon,
        isIncome = isIncome
    )
}