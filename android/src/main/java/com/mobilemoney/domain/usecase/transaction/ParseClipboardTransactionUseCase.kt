package com.mobilemoney.domain.usecase.transaction

import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.model.ParsedTransaction
import com.mobilemoney.domain.parser.TextParser
import com.mobilemoney.domain.repository.AccountRepository
import com.mobilemoney.domain.repository.CategoryRepository
import com.mobilemoney.domain.repository.MessageRegexRepository
import com.mobilemoney.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class ParseClipboardTransactionUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val messageRegexRepository: MessageRegexRepository
) {
    data class ClipboardResult(
        val parsed: ParsedTransaction,
        val account: Account,
        val categoryId: UUID?,
        val comment: String,
        val matchedRegex: Boolean = false
    )

    sealed class Result {
        data class Success(val prefill: ClipboardResult) : Result()
        data class NoMatch(val text: String) : Result()
        data class NoAccount(val text: String) : Result()
        data class DebugInfo(
            val clipboardText: String,
            val matched: Boolean,
            val accountName: String?,
            val amount: String?,
            val shop: String?,
            val cardMaskParsed: String?,
            val cardMaskAccount: String?,
            val cardMaskMatches: Boolean,
            val isIncome: Boolean?,
            val balance: String?
        ) : Result()
    }

    suspend fun parse(text: String, debugMode: Boolean = false): Result {
        val allRegexes = messageRegexRepository.getRegexes().first()
        val accounts = accountRepository.getAccounts().first()
        val enabledAccounts = accounts.filter { it.autoCreateEnabled }

        var bestResult: ParsedTransaction? = null
        var bestRegexName: String? = null
        var bestCount = -1

        for (re in allRegexes) {
            val parsed = TextParser.parse(text, re.pattern) ?: continue
            val filled = listOfNotNull(
                parsed.amount.takeIf { it.isNotBlank() },
                parsed.shop.takeIf { it.isNotBlank() },
                parsed.cardMask.takeIf { it.isNotBlank() },
                parsed.balance?.takeIf { it.isNotBlank() }
            ).size
            if (filled > bestCount) {
                bestCount = filled
                bestResult = parsed
                bestRegexName = re.label
            }
        }

        val matchedAccount = bestResult?.cardMask?.let { cm ->
            enabledAccounts.find { it.cardMask == cm }
        }

        if (debugMode) {
            return Result.DebugInfo(
                clipboardText = text,
                matched = bestResult != null,
                accountName = matchedAccount?.name
                    ?: if (bestResult != null) "Счёт не найден" else null,
                amount = bestResult?.amount,
                shop = bestResult?.shop,
                cardMaskParsed = bestResult?.cardMask,
                cardMaskAccount = matchedAccount?.cardMask,
                cardMaskMatches = matchedAccount != null,
                isIncome = bestResult?.isIncome,
                balance = bestResult?.balance
            )
        }

        if (enabledAccounts.isEmpty() || allRegexes.isEmpty()) {
            return Result.NoAccount(text)
        }

        for (re in allRegexes) {
            val parsed = TextParser.parse(text, re.pattern) ?: continue

            val matchingAccount = enabledAccounts.find { it.cardMask == parsed.cardMask }
            if (matchingAccount == null) continue

            val isIncome = parsed.isIncome
            val comment = if (!re.skipBalanceCheck && parsed.balance != null) {
                val currentBalance = accountRepository.getAccountBalance(matchingAccount.id.toString())
                val parsedBalance = parseAmount(parsed.balance)
                val expectedAfter = currentBalance - parseAmount(parsed.amount)
                val discrepancy = expectedAfter - parsedBalance
                if (kotlin.math.abs(discrepancy) > 0.01) {
                    "Баланс расходится: ожидалось ${"%.2f".format(expectedAfter)}, получено ${"%.2f".format(parsedBalance)}"
                } else ""
            } else ""

            var categoryId: UUID? = null
            if (parsed.shop.isNotBlank()) {
                val lastTx = transactionRepository.getLastTransactionByShop(parsed.shop, isIncome)
                categoryId = lastTx?.categoryId
            }

            return Result.Success(
                ClipboardResult(
                    parsed = parsed,
                    account = matchingAccount,
                    categoryId = categoryId,
                    comment = comment
                )
            )
        }

        return Result.NoMatch(text)
    }

    private fun parseAmount(amount: String): Double {
        return amount.replace(",", ".").toDoubleOrNull() ?: 0.0
    }
}
