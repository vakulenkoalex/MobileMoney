package com.mobilemoney.domain.usecase.transaction

import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class GetTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepository.getTransactions()
    }
}