package com.mobilemoney.data.local

import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.model.TransactionOrigin
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
        relatedTransactionId = relatedTransactionId?.let { UUID.fromString(it) },
        shop = shop,
        source = source,
        sourceData = sourceData
    )
}

fun Transaction.toEntity(): TransactionEntity {
    val transactionSource = when (origin) {
        TransactionOrigin.CLIPBOARD -> TransactionSource.CLIPBOARD
        TransactionOrigin.MANUAL -> TransactionSource.MANUAL
    }
    return TransactionEntity(
        id = id.toString(),
        accountId = accountId.toString(),
        categoryId = categoryId.toString(),
        amount = amount,
        date = date,
        comment = comment,
        source = transactionSource,
        sourceData = sourceData,
        creatorId = null,
        relatedTransactionId = relatedTransactionId?.toString(),
        shop = shop,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

fun TransactionUi.toEntity(
    source: TransactionSource = TransactionSource.MANUAL,
    sourceData: String? = null
): TransactionEntity {
    return TransactionEntity(
        id = id.toString(),
        accountId = accountId.toString(),
        categoryId = categoryId.toString(),
        amount = amount,
        date = date,
        comment = comment,
        source = source,
        sourceData = sourceData,
        creatorId = null,
        relatedTransactionId = relatedTransactionId?.toString(),
        shop = shop,
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