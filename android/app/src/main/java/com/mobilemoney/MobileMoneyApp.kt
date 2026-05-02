package com.mobilemoney

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.mobilemoney.data.repository.DatabaseRepository
import com.mobilemoney.data.repository.SyncRepository
import com.mobilemoney.worker.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MobileMoneyApp : Application() {
    lateinit var repository: DatabaseRepository
        private set
    lateinit var syncRepository: SyncRepository
        private set

    var isInitialized = false
        private set

    private lateinit var prefs: SharedPreferences
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        repository = DatabaseRepository(this)
        syncRepository = SyncRepository(this)
        checkAndInitialize()
        enableSync()
    }

    private fun checkAndInitialize() {
        applicationScope.launch {
            kotlinx.coroutines.delay(1500)
            val accounts = repository.getAccounts().first()
            if (accounts.isEmpty()) {
                repository.initializeDefaultData()
            }
            prefs.edit().putBoolean("initialized", true).apply()
            isInitialized = true
        }
    }

    private fun enableSync() {
        if (prefs.getBoolean("sync_enabled", true)) {
            SyncWorker.enqueuePeriodicSync(this)
        }
    }

    fun isFirstRun(): Boolean {
        return !prefs.getBoolean("initialized", false)
    }

    companion object {
        lateinit var instance: MobileMoneyApp
            private set

        fun getRepository(context: Context): DatabaseRepository {
            return (context.applicationContext as MobileMoneyApp).repository
        }

        fun isAppInitialized(context: Context): Boolean {
            return (context.applicationContext as? MobileMoneyApp)?.isInitialized ?: false
        }
    }
}