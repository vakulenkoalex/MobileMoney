package com.mobilemoney.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val typeId: String,
    val currencyCode: String,
    val icon: String,
    val isDefault: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncedAt: Long?,
    val serverReceivedAt: Long?
)