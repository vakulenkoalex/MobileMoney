package com.mobilemoney.data.local

import com.mobilemoney.data.model.TransactionUi
import java.util.UUID

fun TransactionEntity.toUiModel(account: AccountEntity?, category: CategoryEntity?): TransactionUi {
    return TransactionUi(
        id = UUID.fromString(id),
        title = category?.name ?: "Без категории",
        subtitle = account?.name ?: formatDate(date),
        comment = comment ?: "",
        amount = amount,
        currency = account?.currencyCode ?: "₽",
        icon = category?.icon ?: "receipt",
        color = if (category?.isIncome == true) 0xFF2196F3 else 0xFF4CAF50,
        isIncome = category?.isIncome ?: false,
        date = date,
        accountId = accountId.takeIf { it.isNotEmpty() }?.let { UUID.fromString(it) },
        categoryId = categoryId?.let { UUID.fromString(it) },
        relatedTransactionId = relatedTransactionId?.let { UUID.fromString(it) }
    )
}

fun TransactionUi.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id.toString(),
        accountId = accountId?.toString() ?: "",
        categoryId = categoryId?.toString(),
        amount = amount,
        date = date,
        comment = comment,
        source = TransactionSource.MANUAL,
        sourceData = null,
        creatorId = null,
        relatedTransactionId = relatedTransactionId?.toString(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 86400000 -> "Сегодня"
        diff < 172800000 -> "Вчера"
        else -> "Дата"
    }
}