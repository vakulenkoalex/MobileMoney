package com.mobilemoney.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncPushRequestDto(
    val currencies: List<CurrencyDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class CurrencyDto(
    val code: String,
    val name: String,
    val symbol: String,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L
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
    val serverReceivedAt: Long? = null
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    @SerialName("is_income") val isIncome: Boolean = false,
    val icon: String = "",
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

@Serializable
data class TransactionDto(
    val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("category_id") val categoryId: String? = null,
    val amount: Double = 0.0,
    val date: Long = 0L,
    val comment: String = "",
    val source: String = "manual",
    @SerialName("source_data") val sourceData: String? = null,
    @SerialName("creator_id") val creatorId: String? = null,
    @SerialName("related_transaction_id") val relatedTransactionId: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("deleted_at") val deletedAt: Long? = null
)