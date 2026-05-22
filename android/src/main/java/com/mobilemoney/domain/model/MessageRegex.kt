package com.mobilemoney.domain.model

import java.util.UUID

data class MessageRegex(
    val id: UUID = UUID.randomUUID(),
    val pattern: String,
    val skipBalanceCheck: Boolean = false
)
