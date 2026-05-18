package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val isIncome: Boolean,
    val icon: String,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncedAt: Long?,
    val serverReceivedAt: Long?
)