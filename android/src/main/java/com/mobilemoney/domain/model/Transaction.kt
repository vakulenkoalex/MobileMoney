package com.mobilemoney.domain.model

import java.util.UUID

enum class TransactionOrigin {
    MANUAL,
    CLIPBOARD
}

data class Transaction(
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
    val relatedTransactionId: UUID? = null,
    val shop: String? = null,
    val origin: TransactionOrigin = TransactionOrigin.MANUAL,
    val sourceData: String? = null
)