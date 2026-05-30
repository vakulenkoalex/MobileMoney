package com.mobilemoney.domain.repository

import com.mobilemoney.domain.model.Sender
import com.mobilemoney.domain.model.SenderType
import kotlinx.coroutines.flow.Flow

interface SenderRepository {
    fun getSenders(): Flow<List<Sender>>
    suspend fun getSenderById(id: String): Sender?
    suspend fun addSender(sender: String, label: String, type: SenderType)
    suspend fun updateSender(id: String, sender: String, label: String, type: SenderType)
    suspend fun deleteSender(id: String)
}
