package com.mobilemoney.domain.usecase.transaction

import com.mobilemoney.domain.repository.TransactionRepository

class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: String) {
        transactionRepository.deleteTransaction(id)
    }
}