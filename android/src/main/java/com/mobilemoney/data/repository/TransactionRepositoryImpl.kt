package com.mobilemoney.data.repository

import com.mobilemoney.domain.model.TransactionSource
import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val databaseRepository: DatabaseRepository
) : TransactionRepository {

    override fun getTransactions(): Flow<List<Transaction>> {
        return databaseRepository.getTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        return databaseRepository.getTransactionById(id)?.toDomain()
    }

    override suspend fun getRelatedTransaction(relatedId: String, excludeId: String): Transaction? {
        return databaseRepository.getRelatedTransaction(relatedId, excludeId)?.toDomain()
    }

    override suspend fun addTransaction(transaction: Transaction) {
        databaseRepository.addTransaction(transaction.toUiModel())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        databaseRepository.updateTransaction(transaction.toUiModel())
    }

    override suspend fun deleteTransaction(id: String) {
        databaseRepository.deleteTransaction(id)
    }

    override suspend fun splitTransaction(
        originalId: String,
        mainAmount: Double,
        newTransaction: Transaction
    ) {
        databaseRepository.splitTransaction(
            originalId = originalId,
            mainAmount = mainAmount,
            newTransaction = newTransaction.toUiModel()
        )
    }

    override suspend fun getLastTransactionByShop(shop: String, isIncome: Boolean): Transaction? {
        return databaseRepository.getLastTransactionByShop(shop, isIncome)?.toDomain()
    }
}

private fun TransactionUi.toDomain(): Transaction {
    return Transaction(
        id = id,
        title = title,
        subtitle = subtitle,
        comment = comment,
        amount = amount,
        currency = currency,
        icon = icon,
        color = color,
        isIncome = isIncome,
        date = date,
        accountId = accountId,
        categoryId = categoryId,
        relatedTransactionId = relatedTransactionId,
        shop = shop,
        source = source,
        sourceData = sourceData
    )
}

private fun Transaction.toUiModel(): TransactionUi {
    return TransactionUi(
        id = id,
        title = title,
        subtitle = subtitle,
        comment = comment,
        amount = amount,
        currency = currency,
        icon = icon,
        color = color,
        isIncome = isIncome,
        date = date,
        accountId = accountId,
        categoryId = categoryId,
        relatedTransactionId = relatedTransactionId,
        shop = shop,
        source = source,
        sourceData = sourceData
    )
}