package com.mobilemoney.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.local.MessageEntity
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.worker.MessageWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val featurePrefs = FeaturePreferences(context)
                if (!featurePrefs.smsEnabled) return@launch

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return@launch
                val sender = messages.firstOrNull()?.originatingAddress ?: return@launch
                val body = messages.joinToString("") { it.messageBody ?: "" }

                val db = AppDatabase.getDatabase(context)
                val messageDao = db.messageDao()
                val senderDao = db.senderDao()
                val transactionDao = db.transactionDao()

                if (featurePrefs.debugModeEnabled) {
                    messageDao.insert(
                        MessageEntity(sender = sender, body = body, receivedAt = System.currentTimeMillis())
                    )
                    return@launch
                }

                val knownSender = senderDao.findBySender(sender)
                if (knownSender == null) return@launch

                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val exists = transactionDao.countBySourceDataSince(body, todayStart) > 0
                if (exists) return@launch

                messageDao.insert(
                    MessageEntity(sender = sender, body = body, receivedAt = System.currentTimeMillis())
                )

                val workRequest = OneTimeWorkRequestBuilder<MessageWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)

            } finally {
                pendingResult.finish()
            }
        }
    }
}
