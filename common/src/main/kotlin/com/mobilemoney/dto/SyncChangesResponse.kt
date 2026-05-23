package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncChangesResponse(
    val timestamp: Long,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val messageRegexes: List<MessageRegexDto> = emptyList(),
    val senders: List<SenderDto> = emptyList()
)
