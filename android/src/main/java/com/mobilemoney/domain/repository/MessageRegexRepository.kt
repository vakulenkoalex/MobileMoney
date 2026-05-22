package com.mobilemoney.domain.repository

import com.mobilemoney.domain.model.MessageRegex
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface MessageRegexRepository {
    fun getRegexes(): Flow<List<MessageRegex>>
    suspend fun getRegexById(id: UUID): MessageRegex?
    suspend fun saveRegex(regex: MessageRegex)
    suspend fun deleteRegex(id: UUID)
}
