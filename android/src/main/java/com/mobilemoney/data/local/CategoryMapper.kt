package com.mobilemoney.data.local

import com.mobilemoney.data.model.CategoryUi
import java.util.UUID

fun CategoryEntity.toUiModel(): CategoryUi {
    return CategoryUi(
        id = UUID.fromString(id),
        name = name,
        icon = icon,
        isIncome = isIncome,
        isDefault = isDefault,
        parentId = parentId?.let { UUID.fromString(it) }
    )
}

fun CategoryUi.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id.toString(),
        name = name,
        isIncome = isIncome,
        icon = icon,
        parentId = parentId?.toString(),
        isDefault = isDefault,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
