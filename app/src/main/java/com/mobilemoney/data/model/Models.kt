package com.mobilemoney.data.model

import java.util.UUID

data class AccountUi(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val currency: String = "₽",
    val icon: String = "wallet"
)

data class CategoryUi(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val icon: String = "category",
    val isIncome: Boolean = false
)

data class TransactionUi(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val subtitle: String,
    val comment: String = "",
    val amount: Double,
    val currency: String = "₽",
    val icon: String,
    val color: Long,
    val isIncome: Boolean,
    val date: Long = System.currentTimeMillis(),
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val relatedTransactionId: UUID? = null
)