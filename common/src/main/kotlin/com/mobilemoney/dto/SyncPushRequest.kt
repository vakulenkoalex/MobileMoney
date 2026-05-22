package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncPushRequest(
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList(),
    val messageRegexes: List<MessageRegexDto> = emptyList()
)
