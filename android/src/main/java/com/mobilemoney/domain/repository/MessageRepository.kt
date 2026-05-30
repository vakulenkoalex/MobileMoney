package com.mobilemoney.domain.repository

import com.mobilemoney.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessages(): Flow<List<Message>>
    suspend fun getUnprocessedMessages(): List<Message>
    suspend fun markProcessed(id: String, error: String? = null, transactionId: String? = null)
    suspend fun insert(message: Message)
    suspend fun deleteById(id: String)
    suspend fun deleteAll()
}
