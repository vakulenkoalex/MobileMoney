package com.mobilemoney.domain.model

import java.util.UUID

enum class AccountType(val id: String, val displayName: String) {
    CASH("cash", "Наличные"),
    CARD("card", "Банковская карта"),
    ACCOUNT("account", "Счёт")
}

data class Account(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: AccountType = AccountType.CASH,
    val currency: String = "₽",
    val icon: String = "wallet",
    val isDefault: Boolean = false,
    val balance: Double = 0.0
)