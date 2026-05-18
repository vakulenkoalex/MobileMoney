package com.mobilemoney.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mobilemoney.BuildConfig
import com.mobilemoney.data.local.AccountDao
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.local.CategoryDao
import com.mobilemoney.data.local.TransactionDao
import com.mobilemoney.data.remote.LoginResponse
import com.mobilemoney.data.remote.ErrorResponse
import com.mobilemoney.data.remote.SyncApiClient
import com.mobilemoney.dto.AccountDto
import com.mobilemoney.dto.CategoryDto
import com.mobilemoney.dto.TransactionDto
import com.mobilemoney.dto.SyncPushRequest
import com.mobilemoney.dto.SyncChangesResponse
import com.mobilemoney.dto.SyncPullResponse
import com.mobilemoney.dto.SyncPushResponse
import com.mobilemoney.domain.repository.SyncRepository as DomainSyncRepository
import com.mobilemoney.domain.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import android.util.Log

class SyncRepository(context: Context) : DomainSyncRepository {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sync_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val database = AppDatabase.getDatabase(context)
    private val accountDao: AccountDao = database.accountDao()
    private val categoryDao: CategoryDao = database.categoryDao()
    private val transactionDao: TransactionDao = database.transactionDao()
    private val apiClient = SyncApiClient(context)

    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: StateFlow<SyncState> = _syncState

    companion object {
        @Volatile
        private var instance: SyncRepository? = null

        fun getInstance(context: Context): SyncRepository {
            return instance ?: synchronized(this) {
                instance ?: SyncRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    override var serverUrl: String
        get() = prefs.getString("server_url", null) ?: BuildConfig.SERVER_URL
        set(value) = prefs.edit().putString("server_url", value).apply()

    var deviceToken: String?
        get() = prefs.getString("device_token", null)
        set(value) = prefs.edit().putString("device_token", value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong("last_sync_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_sync_timestamp", value).apply()

    var deviceId: String
        get() = prefs.getString("device_id", null) ?: apiClient.getDeviceId().also {
            prefs.edit().putString("device_id", it).apply()
        }
        set(value) = prefs.edit().putString("device_id", value).apply()

    var userLogin: String?
        get() = prefs.getString("user_login", null)
        set(value) = prefs.edit().putString("user_login", value).apply()

    override suspend fun login(login: String, password: String): Result<String> {
        apiClient.setBaseUrl(serverUrl)
        val result = apiClient.login(login, password)
        result.onSuccess { token ->
            deviceToken = token
            userLogin = login
        }
        return result
    }

    suspend fun getUnsyncedAccounts(): List<AccountDto> {
        return accountDao.getUnsyncedAccounts().map { it.toSyncDto() }
    }

    suspend fun getUnsyncedCategories(): List<CategoryDto> {
        return categoryDao.getUnsyncedCategories().map { it.toSyncDto() }
    }

    suspend fun getUnsyncedTransactions(): List<TransactionDto> {
        return transactionDao.getUnsyncedTransactions().map { it.toSyncDto() }
    }

    override suspend fun sync(): Result<Unit> {
        Log.d("SyncRepository", "=== sync() START ===")
        Log.d("SyncRepository", "deviceToken: ${deviceToken?.take(10)}...")

        _syncState.value = _syncState.value.copy(isSyncing = true, error = null)

        apiClient.setBaseUrl(serverUrl)
        deviceToken?.let { apiClient.setToken(it) }

        if (deviceToken == null) {
            Log.d("SyncRepository", "NO TOKEN - early exit")
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                error = "Войдите в аккаунт"
            )
            return Result.failure(Exception("Войдите в аккаунт"))
        }

        val pullResult = pullChanges()
        if (pullResult.isFailure) {
            val errorMsg = pullResult.exceptionOrNull()?.message ?: ""
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                error = errorMsg
            )
            return Result.failure(pullResult.exceptionOrNull() ?: Exception("Pull failed"))
        }

        val pushResult = pushChanges()
        if (pushResult.isFailure) {
            val errorMsg = pushResult.exceptionOrNull()?.message ?: ""
            if (errorMsg.contains("401") || errorMsg.contains("invalid")) {
                logout()
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    error = "Токен невалиден. Войдите снова."
                )
            } else {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    error = errorMsg
                )
            }
            return Result.failure(pushResult.exceptionOrNull() ?: Exception("Push failed"))
        }

        _syncState.value = _syncState.value.copy(
            isSyncing = false,
            lastSyncTime = lastSyncTimestamp
        )

        return Result.success(Unit)
    }

    private suspend fun pushChanges(): Result<Int> {
        val accountDtos = getUnsyncedAccounts()
        val categoryDtos = getUnsyncedCategories()
        val transactionDtos = getUnsyncedTransactions()

        Log.d("SyncRepository", "pushChanges: accounts=${accountDtos.size}, categories=${categoryDtos.size}, transactions=${transactionDtos.size}")

        val request = SyncPushRequest(
            accounts = accountDtos,
            categories = categoryDtos,
            transactions = transactionDtos
        )

        return if (request.accounts.isEmpty() && request.categories.isEmpty() && request.transactions.isEmpty()) {
            Result.success(0)
        } else {
            apiClient.pushChanges(request).map { response ->
                val syncedAt = response.timestamp
                accountDtos.forEach { markAccountSynced(it.id, syncedAt) }
                categoryDtos.forEach { markCategorySynced(it.id, syncedAt) }
                transactionDtos.forEach { markTransactionSynced(it.id, syncedAt) }
                response.synced
            }
        }
    }

    private suspend fun pullChanges(): Result<Unit> {
        val since = lastSyncTimestamp
        Log.d("SyncRepository", "pullChanges: since=$since")

        val hasLocalData = accountDao.getAllAccounts().first().isNotEmpty() ||
                          categoryDao.getAllCategories().first().isNotEmpty()

        val useFullPull = since == 0L || !hasLocalData
        Log.d("SyncRepository", "pullChanges: useFullPull=$useFullPull, hasLocalData=$hasLocalData")

        if (useFullPull) {
            val result = apiClient.pullAll()
            Log.d("SyncRepository", "pullAll result: ${result.isSuccess}")
            result.onFailure { e -> Log.e("SyncRepository", "pullAll error: ${e.message}") }
            return result.map { response ->
                val syncedAt = response.timestamp
                Log.d("SyncRepository", "pullChanges: accounts=${response.accounts.size}, categories=${response.categories.size}, syncedAt=$syncedAt")
                response.accounts.forEach { dto -> upsertAccount(dto, syncedAt) }
                response.categories.forEach { dto -> upsertCategory(dto, syncedAt) }
                response.transactions.forEach { dto -> upsertTransaction(dto, syncedAt) }
            }
        } else {
            val result = apiClient.getChanges(since)
            Log.d("SyncRepository", "getChanges result: ${result.isSuccess}")
            result.onFailure { e -> Log.e("SyncRepository", "getChanges error: ${e.message}") }
            return result.map { response ->
                val syncedAt = response.timestamp
                lastSyncTimestamp = syncedAt
                Log.d("SyncRepository", "pullChanges: accounts=${response.accounts.size}, categories=${response.categories.size}, syncedAt=$syncedAt")
                response.accounts.forEach { dto -> upsertAccount(dto, syncedAt) }
                response.categories.forEach { dto -> upsertCategory(dto, syncedAt) }
                response.transactions.forEach { dto -> upsertTransaction(dto, syncedAt) }
            }
        }
    }

    private suspend fun upsertAccount(dto: AccountDto, syncedAt: Long) {
        val existing = accountDao.getAccountById(dto.id)
        if (existing == null || existing.syncedAt != null) {
            accountDao.insert(dto.toEntity().copy(syncedAt = syncedAt, serverReceivedAt = dto.serverReceivedAt))
        }
    }

    private suspend fun upsertCategory(dto: CategoryDto, syncedAt: Long) {
        val existing = categoryDao.getCategoryById(dto.id)
        if (existing == null || existing.syncedAt != null) {
            categoryDao.insert(dto.toEntity().copy(syncedAt = syncedAt, serverReceivedAt = dto.serverReceivedAt))
        }
    }

    private suspend fun upsertTransaction(dto: TransactionDto, syncedAt: Long) {
        val existing = transactionDao.getTransactionById(dto.id)
        if (existing == null || existing.syncedAt != null) {
            transactionDao.insert(dto.toEntity().copy(syncedAt = syncedAt, serverReceivedAt = dto.serverReceivedAt))
        }
    }

    suspend fun markAccountSynced(id: String, syncedAt: Long = System.currentTimeMillis()) {
        accountDao.markSynced(id, syncedAt)
    }

    suspend fun markCategorySynced(id: String, syncedAt: Long = System.currentTimeMillis()) {
        categoryDao.markSynced(id, syncedAt)
    }

    suspend fun markTransactionSynced(id: String, syncedAt: Long = System.currentTimeMillis()) {
        transactionDao.markSynced(id, syncedAt)
    }

    override fun logout() {
        deviceToken = null
        userLogin = null
        lastSyncTimestamp = 0L
    }

    override fun isLoggedIn(): Boolean = deviceToken != null

    override suspend fun ping(): Result<Unit> {
        apiClient.setBaseUrl(serverUrl)
        return apiClient.ping()
    }
}