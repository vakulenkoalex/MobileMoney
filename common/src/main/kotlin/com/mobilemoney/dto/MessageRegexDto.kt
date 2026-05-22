package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageRegexDto(
    val id: String,
    val pattern: String,
    val skipBalanceCheck: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)
