package com.mobilemoney.data.repository

import com.mobilemoney.data.local.MessageRegexDao
import com.mobilemoney.data.local.toUiModel
import com.mobilemoney.data.local.toEntity
import com.mobilemoney.data.model.MessageRegexUi
import com.mobilemoney.domain.model.MessageRegex
import com.mobilemoney.domain.repository.MessageRegexRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class MessageRegexRepositoryImpl(
    private val dao: MessageRegexDao
) : MessageRegexRepository {

    override fun getRegexes(): Flow<List<MessageRegex>> {
        return dao.getAll().map { list ->
            list.map { it.toUiModel().toDomain() }
        }
    }

    override suspend fun getRegexById(id: UUID): MessageRegex? {
        return dao.getById(id.toString())?.toUiModel()?.toDomain()
    }

    override suspend fun saveRegex(regex: MessageRegex) {
        dao.insert(regex.toUiModel().toEntity())
    }

    override suspend fun deleteRegex(id: UUID) {
        dao.softDelete(id.toString(), System.currentTimeMillis())
    }
}

private fun MessageRegexUi.toDomain(): MessageRegex {
    return MessageRegex(
        id = id,
        label = label,
        pattern = pattern,
        skipBalanceCheck = skipBalanceCheck
    )
}

private fun MessageRegex.toUiModel(): MessageRegexUi {
    return MessageRegexUi(
        id = id,
        label = label,
        pattern = pattern,
        skipBalanceCheck = skipBalanceCheck
    )
}
