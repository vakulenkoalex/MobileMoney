package com.mobilemoney

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MobileMoneyApp : Application() {

    var isInitialized = false
        private set

    private lateinit var prefs: SharedPreferences
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        prefs = createEncryptedSharedPreferences()

        checkAndInitialize()
        enableSync()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            "message_processing",
            "Обработка сообщений",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun createEncryptedSharedPreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                this,
                "app_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("MobileMoneyApp", "Failed to create EncryptedSharedPreferences, clearing data", e)
            try {
                deleteSharedPreferences("app_prefs")
            } catch (clearException: Exception) {
                Log.e("MobileMoneyApp", "Failed to clear preferences", clearException)
            }
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                this,
                "app_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private fun checkAndInitialize() {
        applicationScope.launch {
            kotlinx.coroutines.delay(1500)
            prefs.edit().putBoolean("initialized", true).apply()
            isInitialized = true
        }
    }

    private fun enableSync() {
        // if (prefs.getBoolean("sync_enabled", true)) {
        //     SyncWorker.enqueuePeriodicSync(this)
        // }
    }

    fun isFirstRun(): Boolean {
        return !prefs.getBoolean("initialized", false)
    }

    companion object {
        lateinit var instance: MobileMoneyApp
            private set

    }
}