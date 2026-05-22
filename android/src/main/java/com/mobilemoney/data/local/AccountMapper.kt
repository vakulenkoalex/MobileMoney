package com.mobilemoney.data.local

import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.model.AccountType
import java.util.UUID

fun AccountEntity.toUiModel(balance: Double = 0.0): AccountUi {
    return AccountUi(
        id = UUID.fromString(id),
        name = name,
        type = AccountType.entries.find { it.id == typeId } ?: AccountType.CASH,
        currency = currencyCode,
        icon = icon,
        isDefault = isDefault,
        balance = balance,
        autoCreateEnabled = autoCreateEnabled,
        cardMask = cardMask
    )
}

fun AccountUi.toEntity(): AccountEntity {
    return AccountEntity(
        id = id.toString(),
        name = name,
        typeId = type.id,
        currencyCode = currency,
        icon = icon,
        isDefault = isDefault,
        archived = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        autoCreateEnabled = autoCreateEnabled,
        cardMask = cardMask
    )
}