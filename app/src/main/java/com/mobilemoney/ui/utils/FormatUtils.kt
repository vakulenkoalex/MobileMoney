package com.mobilemoney.ui.utils

import java.text.DecimalFormat

object FormatUtils {
    private val amountFormat = DecimalFormat("#,##0.00")

    fun formatAmount(amount: Double, currency: String, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign${amountFormat.format(amount)} $currency"
    }

    fun formatBalance(amount: Double, currency: String): String {
        return "${amountFormat.format(amount)} $currency"
    }
}