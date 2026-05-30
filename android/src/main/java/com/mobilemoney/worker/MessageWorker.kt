package com.mobilemoney.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobilemoney.domain.repository.MessageRepository
import com.mobilemoney.domain.usecase.transaction.ProcessSmsTransactionUseCase
import kotlinx.coroutines.flow.first

class MessageWorker(
    context: Context,
    params: WorkerParameters,
    private val processSmsUseCase: ProcessSmsTransactionUseCase,
    private val messageRepository: MessageRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val messages = messageRepository.getUnprocessedMessages()

        if (messages.isEmpty()) return Result.success()

        for (message in messages) {
            try {
                when (val result = processSmsUseCase(message)) {
                    is ProcessSmsTransactionUseCase.Result.Success -> {
                        showSuccessNotification("Создана операция: ${"%.2f".format(result.amount)}р в ${result.shop}")
                    }
                    is ProcessSmsTransactionUseCase.Result.ParseFailed -> {
                        messageRepository.markProcessed(message.id, error = "parse_failed")
                        showErrorNotification("Не удалось обработать сообщение: не найден формат")
                    }
                    is ProcessSmsTransactionUseCase.Result.AccountNotFound -> {
                        messageRepository.markProcessed(message.id, error = "account_not_found")
                        showErrorNotification("Не удалось обработать сообщение: кошелёк ****${result.cardMask} не найден")
                    }
                    is ProcessSmsTransactionUseCase.Result.SaveFailed -> {
                        messageRepository.markProcessed(message.id, error = "save_failed")
                        showErrorNotification("Ошибка сохранения: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MessageWorker", "Error processing message ${message.id}", e)
                messageRepository.markProcessed(message.id, error = "save_failed")
            }
        }

        return Result.success()
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
