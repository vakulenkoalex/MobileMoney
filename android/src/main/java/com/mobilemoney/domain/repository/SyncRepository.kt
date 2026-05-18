package com.mobilemoney.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class SyncState(
    val isSyncing: Boolean = false,
    val error: String? = null,
    val lastSyncTime: Long = 0L
)

interface SyncRepository {
    val syncState: StateFlow<SyncState>
    fun isLoggedIn(): Boolean
    suspend fun login(login: String, password: String): Result<String>
    suspend fun sync(): Result<Unit>
    fun logout()
    suspend fun ping(): Result<Unit>
    var serverUrl: String
}