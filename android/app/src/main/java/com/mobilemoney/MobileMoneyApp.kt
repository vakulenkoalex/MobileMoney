package com.mobilemoney

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            this,
            "app_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        checkAndInitialize()
        enableSync()
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

        fun isAppInitialized(context: Context): Boolean {
            return (context.applicationContext as? MobileMoneyApp)?.isInitialized ?: false
        }
    }
}