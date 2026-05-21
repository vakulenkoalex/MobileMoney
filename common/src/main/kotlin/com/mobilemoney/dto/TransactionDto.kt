package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val date: Long,
    val comment: String?,
    val source: String,
    val sourceData: String?,
    val creatorId: String?,
    val relatedTransactionId: String?,
    val shop: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncedAt: Long?,
    val serverReceivedAt: Long?
)