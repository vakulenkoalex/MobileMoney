package com.mobilemoney.domain.repository

import com.mobilemoney.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun getDefaultCategory(isIncome: Boolean): Category?
    suspend fun clearDefaultCategories(isIncome: Boolean)
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: String)
    fun getRootCategories(isIncome: Boolean): Flow<List<Category>>
    fun getSubcategories(parentId: String): Flow<List<Category>>
}
