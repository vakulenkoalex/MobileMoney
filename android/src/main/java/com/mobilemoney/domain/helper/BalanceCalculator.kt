package com.mobilemoney.domain.helper

object BalanceCalculator {
    fun calculateBalance(
        transactions: List<TransactionBalanceData>,
        getIsIncome: (categoryId: String?) -> Boolean
    ): Double {
        return transactions.sumOf { tx ->
            if (getIsIncome(tx.categoryId)) tx.amount else -tx.amount
        }
    }

    data class TransactionBalanceData(
        val amount: Double,
        val categoryId: String?
    )
}
