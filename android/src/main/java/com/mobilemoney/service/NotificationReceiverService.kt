package com.mobilemoney.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mobilemoney.data.local.SenderType
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.processor.MessageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiverService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val featurePrefs = FeaturePreferences(this)
        if (!featurePrefs.pushEnabled) return

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

                MessageProcessor.process(
                    context = this@NotificationReceiverService,
                    senderId = sbn.packageName,
                    body = body,
                    debugMode = featurePrefs.debugModeEnabled,
                ) { senderDao ->
                    val knownSender = senderDao.findBySender(sbn.packageName) ?: return@process false
                    val type = SenderType.valueOf(knownSender.type)
                    type in listOf(SenderType.PACKAGE_NAME, SenderType.MESSENGER_PACKAGE_NAME) &&
                        (type != SenderType.MESSENGER_PACKAGE_NAME ||
                         senderDao.findByType(SenderType.MESSENGER_USERNAME.name)
                             .any { it.sender.equals(title, ignoreCase = true) })
                }

            } catch (e: Exception) {
                Log.e("NotificationReceiverService", "Error processing notification", e)
            }
        }
    }
}
