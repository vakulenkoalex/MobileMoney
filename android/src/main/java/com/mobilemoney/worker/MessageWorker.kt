package com.mobilemoney.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobilemoney.data.local.TransactionSource
import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.data.parser.ParsedTextData
import com.mobilemoney.data.parser.TextParser
import com.mobilemoney.di.DI
import kotlinx.coroutines.flow.first

class MessageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dbRepo = DI.databaseRepository
        val messageDao = DI.database.messageDao()
        val regexDao = DI.database.messageRegexDao()

        val unprocessed = dbRepo.getUnprocessedMessages()
        if (unprocessed.isEmpty()) return Result.success()

        for (message in unprocessed) {
            try {
                processMessage(message, dbRepo, regexDao)
            } catch (e: Exception) {
                Log.e("MessageWorker", "Error processing message ${message.id}", e)
                messageDao.markProcessed(message.id, "save_failed")
            }
        }

        return Result.success()
    }

    private suspend fun processMessage(
        message: com.mobilemoney.data.local.MessageEntity,
        dbRepo: com.mobilemoney.data.repository.DatabaseRepository,
        regexDao: com.mobilemoney.data.local.MessageRegexDao
    ): Boolean {
        val messageDao = DI.database.messageDao()

        val allRegexes = regexDao.getAll().first()

        var parsed: ParsedTextData? = null
        for (re in allRegexes) {
            parsed = TextParser.parse(message.body, re.pattern)
            if (parsed != null) break
        }

        if (parsed == null) {
            messageDao.markProcessed(message.id, "parse_failed")
            showErrorNotification("Не удалось обработать сообщение: не найден формат")
            return false
        }

        val account = dbRepo.getAccountByCardMask(parsed.cardMask)
        if (account == null) {
            messageDao.markProcessed(message.id, "account_not_found")
            showErrorNotification("Не удалось обработать сообщение: кошелёк ****${parsed.cardMask} не найден")
            return false
        }

        val lastTx = dbRepo.getLastTransactionByShop(parsed.shop)
        val defaultCategory = dbRepo.getDefaultCategory(isIncome = false)
        val categoryId = lastTx?.categoryId ?: defaultCategory?.id

        val balance = parsed.balance
        val comment = if (balance != null) {
            val currentBalance = dbRepo.getAccountBalanceValue(account.id.toString())
            val amount = parseAmount(parsed.amount)
            val expectedAfter = currentBalance - amount
            val parsedBalance = parseAmount(balance)
            val discrepancy = expectedAfter - parsedBalance
            if (kotlin.math.abs(discrepancy) > 0.01) {
                "Баланс расходится: ожидалось ${"%.2f".format(expectedAfter)}, получено ${"%.2f".format(parsedBalance)}"
            } else ""
        } else ""

        val amount = -parseAmount(parsed.amount)

        val source = if (message.sender.contains(".")) TransactionSource.PUSH else TransactionSource.SMS

        val transactionUi = TransactionUi(
            title = lastTx?.title ?: parsed.shop,
            subtitle = account.name,
            comment = comment,
            amount = amount,
            currency = account.currency,
            icon = lastTx?.icon ?: "receipt",
            color = lastTx?.color ?: 0xFF4CAF50,
            isIncome = false,
            accountId = account.id,
            categoryId = categoryId,
            shop = parsed.shop,
            source = source,
            sourceData = message.body
        )

        dbRepo.addTransaction(transactionUi)
        messageDao.markProcessed(message.id, transactionId = transactionUi.id.toString())

        showSuccessNotification("Создана операция: ${"%.2f".format(amount)}р в ${parsed.shop}")
        return true
    }

    private fun parseAmount(amount: String): Double {
        return amount.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun showErrorNotification(message: String) {
        showNotification("Обработка", message)
    }

    private fun showSuccessNotification(message: String) {
        showNotification("Обработка", message)
    }

    private fun showNotification(title: String, message: String) {
        try {
            val channelId = "message_processing"
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                channelId,
                "Обработка сообщений",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e("MessageWorker", "Failed to show notification", e)
        }
    }
}
