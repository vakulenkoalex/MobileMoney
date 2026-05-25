package com.mobilemoney.data.local

import com.mobilemoney.data.model.MessageRegexUi
import java.util.UUID

fun MessageRegexEntity.toUiModel(): MessageRegexUi {
    return MessageRegexUi(
        id = UUID.fromString(id),
        label = label,
        pattern = pattern,
        skipBalanceCheck = skipBalanceCheck
    )
}

fun MessageRegexUi.toEntity(): MessageRegexEntity {
    return MessageRegexEntity(
        id = id.toString(),
        label = label,
        pattern = pattern,
        skipBalanceCheck = skipBalanceCheck,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
