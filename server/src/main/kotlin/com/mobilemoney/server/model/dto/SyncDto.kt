package com.mobilemoney.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncPushRequestDto(
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val timestamp: Long,
    val synced: Int
)

@Serializable
data class SyncChangesResponse(
    val timestamp: Long,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)