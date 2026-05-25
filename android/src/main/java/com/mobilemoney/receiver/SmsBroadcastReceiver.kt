package com.mobilemoney.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.mobilemoney.data.local.SenderType
import com.mobilemoney.data.repository.FeaturePreferences
import com.mobilemoney.processor.MessageProcessor
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

                MessageProcessor.process(
                    context = context,
                    senderId = sender,
                    body = body,
                    debugMode = featurePrefs.debugModeEnabled,
                ) { senderDao ->
                    val knownSender = senderDao.findBySender(sender)
                    knownSender != null && SenderType.valueOf(knownSender.type) == SenderType.PHONE_NUMBER
                }

            } finally {
                pendingResult.finish()
            }
        }
    }
}
