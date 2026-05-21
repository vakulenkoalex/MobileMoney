package com.mobilemoney.data.model

import com.mobilemoney.data.local.TransactionSource
import java.util.UUID

enum class AccountType(val id: String, val displayName: String) {
    CASH("cash", "Наличные"),
    CARD("card", "Банковская карта"),
    ACCOUNT("account", "Счёт")
}

data class AccountUi(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: AccountType = AccountType.CASH,
    val currency: String = "₽",
    val icon: String = "wallet",
    val isDefault: Boolean = false,
    val balance: Double = 0.0,
    val autoCreateEnabled: Boolean = false,
    val cardMask: String? = null,
    val regexForText: String? = null
)

data class CategoryUi(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val icon: String = "category",
    val isIncome: Boolean = false
)

data class TransactionUi(
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
    val source: TransactionSource = TransactionSource.MANUAL,
    val sourceData: String? = null
)