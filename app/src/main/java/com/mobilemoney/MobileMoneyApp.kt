package com.mobilemoney

import android.app.Application
import android.content.Context
import com.mobilemoney.data.repository.DatabaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MobileMoneyApp : Application() {
    lateinit var repository: DatabaseRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = DatabaseRepository(this)
        initializeDefaultData()
    }

    private fun initializeDefaultData() {
        applicationScope.launch {
            val accounts = repository.getAccounts().first()
            if (accounts.isEmpty()) {
                repository.initializeDefaultData()
            }
        }
    }

    companion object {
        lateinit var instance: MobileMoneyApp
            private set

        fun getRepository(context: Context): DatabaseRepository {
            return (context.applicationContext as MobileMoneyApp).repository
        }
    }
}