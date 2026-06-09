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

        if (body.isBlank()) return
        if (!debugMode && !validateSender(db.senderDao())) return

        messageDao.insert(
            MessageEntity(sender = senderId, body = body, receivedAt = System.currentTimeMillis())
        )

        if (debugMode) return
        if (!FeaturePreferences(context).messageProcessingEnabled) return

        val workRequest = OneTimeWorkRequestBuilder<MessageWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
