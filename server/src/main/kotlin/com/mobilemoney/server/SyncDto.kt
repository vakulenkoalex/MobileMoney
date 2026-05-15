package com.mobilemoney.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncPushRequestDto(
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val typeId: String = "cash",
    val currencyCode: String? = null,
    val icon: String = "",
    val isDefault: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val isIncome: Boolean = false,
    val icon: String = "",
    val parentId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val categoryId: String? = null,
    val amount: Double = 0.0,
    val date: Long = 0L,
    val comment: String = "",
    val source: String = "manual",
    val sourceData: String? = null,
    val creatorId: String? = null,
    val relatedTransactionId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)