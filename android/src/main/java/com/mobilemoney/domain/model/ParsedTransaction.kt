package com.mobilemoney.domain.model

data class ParsedTransaction(
    val amount: String,
    val shop: String,
    val cardMask: String,
    val isIncome: Boolean = false,
    val balance: String? = null
)
