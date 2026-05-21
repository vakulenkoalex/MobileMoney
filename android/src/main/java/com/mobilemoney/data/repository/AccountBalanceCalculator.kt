package com.mobilemoney.data.repository

import com.mobilemoney.data.local.TransactionDao

class AccountBalanceCalculator(
    private val transactionDao: TransactionDao
) {
    suspend fun getAccountBalance(accountId: String): Double {
        return transactionDao.getAccountBalance(accountId) ?: 0.0
    }

    fun calculateExpectedBalance(
        messageBalance: String,
        operationAmount: String
    ): Double? {
        val msgBalance = messageBalance.replace(",", ".").toDoubleOrNull() ?: return null
        val opAmount = operationAmount.replace(",", ".").toDoubleOrNull() ?: return null
        return msgBalance + opAmount
    }

    fun getBalanceDiscrepancy(
        currentBalance: Double,
        expectedBalance: Double
    ): Double? {
        val discrepancy = currentBalance - expectedBalance
        return if (discrepancy == 0.0) null else discrepancy
    }

    fun formatDiscrepancyComment(discrepancy: Double, expected: Double, actual: Double): String {
        return "Баланс расходится: ожидалось %.2f, получено %.2f".format(expected, actual)
    }
}