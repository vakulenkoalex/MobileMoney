package com.mobilemoney.data.repository

import com.mobilemoney.data.local.AccountEntity
import com.mobilemoney.data.local.CategoryEntity
import com.mobilemoney.data.local.TransactionEntity
import com.mobilemoney.data.local.TransactionSource
import com.mobilemoney.dto.AccountDto
import com.mobilemoney.dto.CategoryDto
import com.mobilemoney.dto.TransactionDto

fun AccountEntity.toSyncDto(): AccountDto {
    return AccountDto(
        id = id,
        name = name,
        typeId = typeId,
        currencyCode = currencyCode,
        icon = icon,
        isDefault = isDefault,
        archived = archived,
        autoCreateEnabled = autoCreateEnabled,
        cardMask = cardMask,
        regexForText = regexForText,
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
        source = source.name,
        sourceData = sourceData,
        creatorId = creatorId,
        relatedTransactionId = relatedTransactionId,
        shop = shop,
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
        archived = archived,
        autoCreateEnabled = autoCreateEnabled,
        cardMask = cardMask,
        regexForText = regexForText,
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
        comment = comment ?: "",
        source = TransactionSource.valueOf(source.uppercase()),
        sourceData = sourceData,
        creatorId = creatorId,
        relatedTransactionId = relatedTransactionId,
        shop = shop,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        syncedAt = syncedAt,
        serverReceivedAt = serverReceivedAt
    )
}