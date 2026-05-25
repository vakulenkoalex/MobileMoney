package com.mobilemoney.processor

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.local.MessageEntity
import com.mobilemoney.data.local.SenderDao
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.worker.MessageWorker
import java.util.Calendar

object MessageProcessor {

    suspend fun process(
        context: Context,
        senderId: String,
        body: String,
        debugMode: Boolean,
        validateSender: suspend (SenderDao) -> Boolean = { true },
    ) {
        val db = AppDatabase.getDatabase(context)
        val messageDao = db.messageDao()
        val transactionDao = db.transactionDao()

        if (debugMode) {
            messageDao.insert(
                MessageEntity(sender = senderId, body = body, receivedAt = System.currentTimeMillis())
            )
            return
        }

        if (body.isBlank()) return
        if (!validateSender(db.senderDao())) return

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        if (transactionDao.countBySourceDataSince(body, todayStart) > 0) return

        messageDao.insert(
            MessageEntity(sender = senderId, body = body, receivedAt = System.currentTimeMillis())
        )

        if (!FeaturePreferences(context).messageProcessingEnabled) return

        val workRequest = OneTimeWorkRequestBuilder<MessageWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
