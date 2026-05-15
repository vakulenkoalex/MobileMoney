package com.mobilemoney.data.repository

import com.mobilemoney.data.local.AccountEntity
import com.mobilemoney.data.local.CategoryEntity
import com.mobilemoney.data.local.TransactionEntity
import com.mobilemoney.data.local.TransactionSource
import com.mobilemoney.data.remote.AccountDto
import com.mobilemoney.data.remote.CategoryDto
import com.mobilemoney.data.remote.TransactionDto

fun AccountEntity.toSyncDto(): AccountDto {
    return AccountDto(
        id = id,
        name = name,
        typeId = typeId,
        currencyCode = currencyCode,
        icon = icon,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        serverReceivedAt = serverReceivedAt
    )
}

fun CategoryEntity.toSyncDto(): CategoryDto {
    return CategoryDto(
        id = id,
        name = name,
        isIncome = isIncome,
        icon = icon,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        serverReceivedAt = serverReceivedAt
    )
}

fun TransactionEntity.toSyncDto(): TransactionDto {
    return TransactionDto(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        amount = amount,
        date = date,
        comment = comment,
        creatorId = creatorId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        serverReceivedAt = serverReceivedAt
    )
}

fun AccountDto.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        typeId = typeId,
        currencyCode = currencyCode,
        icon = icon,
        isDefault = isDefault,
        archived = deletedAt != null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        serverReceivedAt = serverReceivedAt
    )
}

fun CategoryDto.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        isIncome = isIncome,
        icon = icon,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        serverReceivedAt = serverReceivedAt
    )
}

fun TransactionDto.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        amount = amount,
        date = date,
        comment = comment,
        source = TransactionSource.MANUAL,
        sourceData = null,
        creatorId = creatorId,
        relatedTransactionId = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        serverReceivedAt = serverReceivedAt
    )
}