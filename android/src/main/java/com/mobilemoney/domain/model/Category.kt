package com.mobilemoney.domain.model

import java.util.UUID

data class Category(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val icon: String = "category",
    val isIncome: Boolean = false,
    val isDefault: Boolean = false,
    val parentId: UUID? = null
)