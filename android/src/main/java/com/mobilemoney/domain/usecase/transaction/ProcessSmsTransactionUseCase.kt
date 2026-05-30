package com.mobilemoney.domain.usecase.transaction

import com.mobilemoney.domain.model.Message
import com.mobilemoney.domain.model.MessageRegex
import com.mobilemoney.domain.model.ParsedTransaction
import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.model.TransactionOrigin
import com.mobilemoney.domain.parser.TextParser
import com.mobilemoney.domain.repository.AccountRepository
import com.mobilemoney.domain.repository.CategoryRepository
import com.mobilemoney.domain.repository.MessageRepository
import com.mobilemoney.domain.repository.TransactionRepository
import com.mobilemoney.domain.repository.MessageRegexRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class ProcessSmsTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val messageRepository: MessageRepository,
    private val messageRegexRepository: MessageRegexRepository
) {
    sealed class Result {
        data class Success(val amount: Double, val shop: String) : Result()
        data class ParseFailed(val error: String) : Result()
        data class AccountNotFound(val cardMask: String) : Result()
        data class SaveFailed(val error: String) : Result()
    }

    suspend operator fun invoke(message: Message): Result {
        val allRegexes = messageRegexRepository.getRegexes().first()

        val parsed = allRegexes.firstNotNullOfOrNull { regex ->
            TextParser.parse(message.body, regex.pattern)
        } ?: return Result.ParseFailed("Не найден формат")

        val account = accountRepository.getAccountByCardMask(parsed.cardMask)
            ?: return Result.AccountNotFound(parsed.cardMask)

        val isIncome = parsed.isIncome
        val lastTx = transactionRepository.getLastTransactionByShop(parsed.shop, isIncome)
        val defaultCategory = categoryRepository.getDefaultCategory(isIncome = isIncome)
        val categoryId = lastTx?.categoryId ?: defaultCategory?.id

        val amount = parseAmount(parsed.amount)

        val balance = parsed.balance
        val comment = if (balance != null) {
            val currentBalance = accountRepository.getAccountBalance(account.id.toString())
            val expectedAfter = currentBalance - amount
            val parsedBalance = parseAmount(balance)
            val discrepancy = expectedAfter - parsedBalance
            if (kotlin.math.abs(discrepancy) > 0.01) {
                "Баланс расходится: ожидалось ${"%.2f".format(expectedAfter)}, получено ${"%.2f".format(parsedBalance)}"
            } else ""
        } else ""

        return try {
            val transaction = Transaction(
                title = lastTx?.title ?: parsed.shop,
                subtitle = account.name,
                comment = comment,
                amount = amount,
                currency = account.currency,
                icon = lastTx?.icon ?: "receipt",
                color = lastTx?.color ?: 0xFF4CAF50,
                isIncome = isIncome,
                accountId = account.id,
                categoryId = categoryId,
                shop = parsed.shop,
                origin = TransactionOrigin.MANUAL,
                sourceData = message.body
            )
            transactionRepository.addTransaction(transaction)
            messageRepository.markProcessed(message.id, transactionId = transaction.id.toString())
            Result.Success(amount = amount, shop = parsed.shop)
        } catch (e: Exception) {
            messageRepository.markProcessed(message.id, error = "save_failed")
            Result.SaveFailed(e.message ?: "Ошибка сохранения")
        }
    }

    private fun parseAmount(amount: String): Double {
        return amount.replace(",", ".").toDoubleOrNull() ?: 0.0
    }
}
