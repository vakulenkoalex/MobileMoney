package com.mobilemoney.data.remote

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.mobilemoney.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun login(login: String, password: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val deviceId = getDeviceId()
                val deviceName = android.os.Build.MODEL

                val url = URL("$baseUrl/api/v1/auth/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 30000
                conn.readTimeout = 30000

                val body = """{"login":"$login","password":"$password","device_id":"$deviceId","device_name":"$deviceName"}"""
                conn.outputStream.use { it.write(body.toByteArray()) }

                val responseCode = conn.responseCode
                val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

                if (responseCode == 200) {
                    val loginResponse = json.decodeFromString<LoginResponse>(response)
                    deviceToken = loginResponse.token
                    Result.success(loginResponse.token)
                } else {
                    val error = try {
                        json.decodeFromString<ErrorResponse>(response).error
                    } catch (e: Exception) {
                        "Login failed: $responseCode"
                    }
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Result.failure(Exception("500: ${e.message}"))
            }
        }
    }

    suspend fun getChanges(since: Long): Result<SyncChangesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = deviceToken ?: return@withContext Result.failure(Exception("Not registered"))
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
                    Result.failure(Exception("$responseCode: Failed to get changes"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("500: ${e.message}"))
            }
        }
    }

    suspend fun pushChanges(request: SyncPushRequest): Result<SyncPushResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SyncApiClient", "pushChanges START - baseUrl: $baseUrl, hasToken: ${deviceToken != null}")

                val token = deviceToken ?: return@withContext Result.failure(Exception("Not registered"))
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
                Log.d("SyncApiClient", "pushChanges responseCode: $responseCode")
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                Log.d("SyncApiClient", "pushChanges response: $response")

                if (responseCode == 200) {
                    Result.success(json.decodeFromString<SyncPushResponse>(response))
                } else {
                    Result.failure(Exception("$responseCode: Failed to push changes"))
                }
            } catch (e: Exception) {
                Log.e("SyncApiClient", "pushChanges exception: ${e.message}", e)
                Result.failure(Exception("500: ${e.message}"))
            }
        }
    }

    suspend fun pullAll(): Result<SyncPullResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = deviceToken ?: return@withContext Result.failure(Exception("Not registered"))
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
                    Result.failure(Exception("$responseCode: Failed to pull"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("500: ${e.message}"))
            }
        }
    }

    fun setToken(token: String) {
        deviceToken = token
    }

    fun getToken(): String? = deviceToken

    suspend fun ping(): Result<Unit> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("SyncApiClient", "Ping to: $baseUrl/")
                val url = URL("$baseUrl/")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val responseCode = conn.responseCode
                android.util.Log.d("SyncApiClient", "Response code: $responseCode")
                if (responseCode == 200) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("$responseCode: Server unreachable"))
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncApiClient", "Ping exception: ${e.message}", e)
                Result.failure(Exception("500: ${e.message}"))
            }
        }
    }
}

@Serializable
data class LoginResponse(
    val token: String,
    val login: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class SyncChangesResponse(
    val timestamp: Long,
    val currencies: List<CurrencyDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class SyncPullResponse(
    val timestamp: Long,
    val currencies: List<CurrencyDto> = emptyList(),
    val accounts: List<AccountDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val transactions: List<TransactionDto> = emptyList()
)

@Serializable
data class SyncPushRequest(
    val currencies: List<CurrencyDto> = emptyList(),
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
    @SerialName("type_id") val typeId: String,
    @SerialName("currency_code") val currencyCode: String,
    val icon: String,
    @SerialName("is_default") val isDefault: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
) {
    fun isDefaultAccount(): Boolean = isDefault == 1
}

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    @SerialName("is_income") val isIncome: Int = 0,
    val icon: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
) {
    fun isIncomeCategory(): Boolean = isIncome == 1
}

@Serializable
data class TransactionDto(
    val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("category_id") val categoryId: String?,
    val amount: Double,
    val date: Long,
    val comment: String,
    @SerialName("creator_id") val creatorId: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

@Serializable
data class CurrencyDto(
    val code: String,
    val name: String,
    val symbol: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)