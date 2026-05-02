package com.mobilemoney.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mobilemoney.BuildConfig
import com.mobilemoney.data.local.AccountDao
import com.mobilemoney.data.local.AccountEntity
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.local.CategoryDao
import com.mobilemoney.data.local.CategoryEntity
import com.mobilemoney.data.local.TransactionDao
import com.mobilemoney.data.local.TransactionEntity
import com.mobilemoney.data.remote.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SyncRepository(context: Context) {
    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(context)
    private val accountDao: AccountDao = database.accountDao()
    private val categoryDao: CategoryDao = database.categoryDao()
    private val transactionDao: TransactionDao = database.transactionDao()
    private val apiClient = SyncApiClient(context)

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState

    var serverUrl: String
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

    var isRegistered: Boolean
        get() = prefs.getBoolean("is_registered", false)
        set(value) = prefs.edit().putBoolean("is_registered", value).apply()

    var userLogin: String?
        get() = prefs.getString("user_login", null)
        set(value) = prefs.edit().putString("user_login", value).apply()

    suspend fun login(login: String, password: String): Result<String> {
        apiClient.setBaseUrl(serverUrl)
        val result = apiClient.login(login, password)
        result.onSuccess { token ->
            deviceToken = token
            userLogin = login
            isRegistered = true
        }
        return result
    }

    suspend fun getUnsyncedAccounts(): List<AccountDto> {
        return accountDao.getUnsyncedAccounts().map { entity ->
            AccountDto(
                id = entity.id,
                name = entity.name,
                typeId = entity.typeId,
                currencyCode = entity.currencyCode,
                icon = entity.icon,
                isDefault = entity.isDefault,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt
            )
        }
    }

    suspend fun getUnsyncedCategories(): List<CategoryDto> {
        return categoryDao.getUnsyncedCategories().map { entity ->
            CategoryDto(
                id = entity.id,
                name = entity.name,
                isIncome = entity.isIncome,
                icon = entity.icon,
                parentId = entity.parentId,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt
            )
        }
    }

    suspend fun getUnsyncedTransactions(): List<TransactionDto> {
        return transactionDao.getUnsyncedTransactions().map { entity ->
            TransactionDto(
                id = entity.id,
                accountId = entity.accountId,
                categoryId = entity.categoryId,
                amount = entity.amount,
                date = entity.date,
                comment = entity.comment,
                creatorId = entity.creatorId,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                deletedAt = entity.deletedAt
            )
        }
    }

    suspend fun register(): Result<String> {
        apiClient.setBaseUrl(serverUrl)
        val result = apiClient.register(android.os.Build.MODEL)
        result.onSuccess { token ->
            deviceToken = token
            isRegistered = true
        }
        return result
    }

    suspend fun sync(): Result<Unit> {
        _syncState.value = _syncState.value.copy(isSyncing = true, error = null)

        if (!isRegistered) {
            val regResult = register()
            if (regResult.isFailure) {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    error = regResult.exceptionOrNull()?.message
                )
                return Result.failure(regResult.exceptionOrNull() ?: Exception("Registration failed"))
            }
        }

        apiClient.setBaseUrl(serverUrl)
        deviceToken?.let { apiClient.setToken(it) }

        val pushResult = pushChanges()
        if (pushResult.isFailure) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                error = pushResult.exceptionOrNull()?.message
            )
            return Result.failure(pushResult.exceptionOrNull() ?: Exception("Push failed"))
        }

        val pullResult = pullChanges()
        if (pullResult.isFailure) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                error = pullResult.exceptionOrNull()?.message
            )
            return Result.failure(pullResult.exceptionOrNull() ?: Exception("Pull failed"))
        }

        lastSyncTimestamp = System.currentTimeMillis()
        _syncState.value = _syncState.value.copy(
            isSyncing = false,
            lastSyncTime = lastSyncTimestamp
        )

        return Result.success(Unit)
    }

    private suspend fun pushChanges(): Result<Int> {
        val request = SyncPushRequest(
            accounts = getUnsyncedAccounts(),
            categories = getUnsyncedCategories(),
            transactions = getUnsyncedTransactions()
        )

        return if (request.accounts.isEmpty() && request.categories.isEmpty() && request.transactions.isEmpty()) {
            Result.success(0)
        } else {
            apiClient.pushChanges(request).map { it.synced }
        }
    }

    private suspend fun pullChanges(): Result<Unit> {
        val since = lastSyncTimestamp
        val result = apiClient.getChanges(since)

        return result.map { response ->
            response.accounts.forEach { dto ->
                upsertAccount(dto)
            }
            response.categories.forEach { dto ->
                upsertCategory(dto)
            }
            response.transactions.forEach { dto ->
                upsertTransaction(dto)
            }
        }
    }

    private suspend fun upsertAccount(dto: AccountDto) {
        val existing = accountDao.getAccountById(dto.id)
        if (existing == null || existing.updatedAt < dto.updatedAt) {
            accountDao.insert(
                AccountEntity(
                    id = dto.id,
                    name = dto.name,
                    typeId = dto.typeId,
                    currencyCode = dto.currencyCode,
                    icon = dto.icon,
                    isDefault = dto.isDefault,
                    archived = dto.deletedAt != null,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt
                )
            )
        }
    }

    private suspend fun upsertCategory(dto: CategoryDto) {
        val existing = categoryDao.getCategoryById(dto.id)
        if (existing == null || existing.updatedAt < dto.updatedAt) {
            categoryDao.insert(
                CategoryEntity(
                    id = dto.id,
                    name = dto.name,
                    isIncome = dto.isIncome,
                    icon = dto.icon,
                    parentId = dto.parentId,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt
                )
            )
        }
    }

    private suspend fun upsertTransaction(dto: TransactionDto) {
        val existing = transactionDao.getTransactionById(dto.id)
        if (existing == null || existing.updatedAt < dto.updatedAt) {
            transactionDao.insert(
                TransactionEntity(
                    id = dto.id,
                    accountId = dto.accountId,
                    categoryId = dto.categoryId,
                    amount = dto.amount,
                    date = dto.date,
                    comment = dto.comment,
                    source = com.mobilemoney.data.local.TransactionSource.MANUAL,
                    sourceData = null,
                    creatorId = dto.creatorId,
                    relatedTransactionId = null,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    deletedAt = dto.deletedAt
                )
            )
        }
    }

    suspend fun markAccountSynced(id: String) {
        accountDao.markSynced(id, System.currentTimeMillis())
    }

    suspend fun markCategorySynced(id: String) {
        categoryDao.markSynced(id, System.currentTimeMillis())
    }

    suspend fun markTransactionSynced(id: String) {
        transactionDao.markSynced(id, System.currentTimeMillis())
    }

    fun logout() {
        deviceToken = null
        userLogin = null
        isRegistered = false
        lastSyncTimestamp = 0L
    }

    fun isLoggedIn(): Boolean = deviceToken != null && isRegistered
}

data class SyncState(
    val isSyncing: Boolean = false,
    val error: String? = null,
    val lastSyncTime: Long = 0L
)