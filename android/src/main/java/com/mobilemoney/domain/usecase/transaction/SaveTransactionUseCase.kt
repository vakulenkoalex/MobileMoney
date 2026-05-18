package com.mobilemoney.domain.usecase.transaction

import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.repository.TransactionRepository
import java.util.UUID

class SaveTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    sealed class Result {
        data object Success : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        transaction: Transaction,
        isEditing: Boolean,
        isSplitMode: Boolean = false,
        splitAmount: Double? = null,
        splitCategoryId: UUID? = null
    ): Result {
        try {
            if (isEditing) {
                transactionRepository.deleteTransaction(transaction.id.toString())
            }

            if (isSplitMode && splitAmount != null && splitCategoryId != null) {
                val totalAmount = transaction.amount
                val remainingAmount = totalAmount - splitAmount

                val mainTransaction = transaction.copy(
                    id = UUID.randomUUID(),
                    amount = remainingAmount
                )
                val splitTransaction = transaction.copy(
                    id = UUID.randomUUID(),
                    amount = splitAmount,
                    categoryId = splitCategoryId
                )

                transactionRepository.addTransaction(mainTransaction)
                transactionRepository.addTransaction(splitTransaction)
            } else {
                val id = if (isEditing) transaction.id else UUID.randomUUID()
                transactionRepository.addTransaction(transaction.copy(id = id))
            }

            return Result.Success
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Unknown error")
        }
    }
}