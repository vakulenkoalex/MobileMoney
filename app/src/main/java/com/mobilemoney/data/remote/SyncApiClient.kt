package com.mobilemoney.data.remote

import android.content.Context
import android.provider.Settings
import com.mobilemoney.BuildConfig
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class SyncApiClient(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var baseUrl: String = BuildConfig.SERVER_URL
    private var deviceToken: String? = null

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
    }

    fun register(deviceName: String): Result<String> {
        return try {
            val deviceId = getDeviceId()
            val url = URL("$baseUrl/api/v1/sync/register?deviceId=$deviceId&deviceName=$deviceName")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val responseCode = conn.responseCode
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }

            if (responseCode == 200) {
                val data = json.decodeFromString<RegisterResponse>(response)
                deviceToken = data.token
                Result.success(data.token)
            } else {
                Result.failure(Exception("Registration failed: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChanges(since: Long): Result<SyncChangesResponse> {
        return try {
            val token = deviceToken ?: return Result.failure(Exception("Not registered"))
            val url = URL("$baseUrl/api/v1/sync/changes?since=$since")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", token)
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val responseCode = conn.responseCode
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }

            if (responseCode == 200) {
                Result.success(json.decodeFromString<SyncChangesResponse>(response))
            } else {
                Result.failure(Exception("Failed to get changes: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pushChanges(request: SyncPushRequest): Result<SyncPushResponse> {
        return try {
            val token = deviceToken ?: return Result.failure(Exception("Not registered"))
            val url = URL("$baseUrl/api/v1/sync/push")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", token)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val body = json.encodeToString(SyncPushRequest.serializer(), request)
            conn.outputStream.use { it.write(body.toByteArray()) }

            val responseCode = conn.responseCode
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }

            if (responseCode == 200) {
                Result.success(json.decodeFromString<SyncPushResponse>(response))
            } else {
                Result.failure(Exception("Failed to push changes: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pullAll(): Result<SyncPullResponse> {
        return try {
            val token = deviceToken ?: return Result.failure(Exception("Not registered"))
            val url = URL("$baseUrl/api/v1/sync/pull")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", token)
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val responseCode = conn.responseCode
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }

            if (responseCode == 200) {
                Result.success(json.decodeFromString<SyncPullResponse>(response))
            } else {
                Result.failure(Exception("Failed to pull: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setToken(token: String) {
        deviceToken = token
    }

    fun getToken(): String? = deviceToken
}

@Serializable
data class RegisterResponse(
    val token: String,
    val deviceId: String
)

@Serializable
data class SyncChangesResponse(
    val timestamp: Long,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class SyncPullResponse(
    val timestamp: Long,
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class SyncPushRequest(
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class SyncPushResponse(
    val success: Boolean,
    val timestamp: Long,
    val synced: Int
)

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val typeId: String,
    val currencyCode: String?,
    val icon: String,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val isIncome: Boolean,
    val icon: String,
    val parentId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val categoryId: String?,
    val amount: Double,
    val date: Long,
    val comment: String,
    val creatorId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)