package com.mobilemoney.domain.model

import java.util.UUID

data class MessageRegex(
    val id: UUID = UUID.randomUUID(),
    val label: String,
    val pattern: String,
    val skipBalanceCheck: Boolean = false
)
