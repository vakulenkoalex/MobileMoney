package com.mobilemoney.ui.common

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mobilemoney.service.NotificationReceiverService

object PermissionChecker {

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationListenerAccess(context: Context): Boolean {
        val component = ComponentName(context, NotificationReceiverService::class.java)
        val listeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return listeners != null && listeners.contains(component.flattenToString())
    }
}
