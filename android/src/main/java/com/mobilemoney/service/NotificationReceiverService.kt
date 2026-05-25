package com.mobilemoney.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.local.MessageEntity
import com.mobilemoney.data.local.SenderType
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.worker.MessageWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationReceiverService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val featurePrefs = FeaturePreferences(this)
        if (!featurePrefs.pushEnabled) return

        val packageName = sbn.packageName

        val db = AppDatabase.getDatabase(this)
        val senderDao = db.senderDao()
        val messageDao = db.messageDao()
        val transactionDao = db.transactionDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val extras: Bundle = sbn.notification.extras ?: return@launch
                val title = extras.getCharSequence(Notification.EXTRA_TITLE, "").toString()
                val body = buildString {
                    append(title)
                    listOf(
                        Notification.EXTRA_TEXT,
                        Notification.EXTRA_BIG_TEXT
                    ).forEach { key ->
                        extras.getCharSequence(key, "")
                            ?.toString()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { append("\n$it") }
                    }
                }

                if (featurePrefs.debugModeEnabled) {
                    messageDao.insert(
                        MessageEntity(sender = packageName, body = body, receivedAt = System.currentTimeMillis())
                    )
                    return@launch
                }

                val knownSender = senderDao.findBySender(packageName)
                if (knownSender == null || SenderType.valueOf(knownSender.type) !in listOf(SenderType.PACKAGE_NAME, SenderType.MESSENGER_PACKAGE_NAME)) return@launch

                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val exists = transactionDao.countBySourceDataSince(body, todayStart) > 0
                if (exists) return@launch

                messageDao.insert(
                    MessageEntity(sender = packageName, body = body, receivedAt = System.currentTimeMillis())
                )

                val workRequest = OneTimeWorkRequestBuilder<MessageWorker>().build()
                WorkManager.getInstance(this@NotificationReceiverService).enqueue(workRequest)

            } catch (e: Exception) {
                Log.e("NotificationReceiverService", "Error processing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // nothing
    }
}
