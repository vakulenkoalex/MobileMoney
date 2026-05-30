package com.mobilemoney.data.repository

import com.mobilemoney.data.local.MessageEntity
import com.mobilemoney.domain.model.Message
import com.mobilemoney.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(
    private val databaseRepository: DatabaseRepository
) : MessageRepository {

    override fun getMessages(): Flow<List<Message>> {
        return databaseRepository.getMessages().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getUnprocessedMessages(): List<Message> {
        return databaseRepository.getUnprocessedMessages().map { it.toDomain() }
    }

    override suspend fun markProcessed(id: String, error: String?, transactionId: String?) {
        databaseRepository.markMessageProcessed(id, error, transactionId)
    }

    override suspend fun insert(message: Message) {
        databaseRepository.insertMessage(message.toEntity())
    }

    override suspend fun deleteById(id: String) {
        databaseRepository.deleteMessageById(id)
    }

    override suspend fun deleteAll() {
        databaseRepository.deleteAllMessages()
    }
}

private fun MessageEntity.toDomain(): Message {
    return Message(
        id = id,
        sender = sender,
        body = body,
        receivedAt = receivedAt,
        processed = processed,
        error = error,
        transactionId = transactionId,
        createdAt = createdAt
    )
}

private fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        sender = sender,
        body = body,
        receivedAt = receivedAt,
        processed = processed,
        error = error,
        transactionId = transactionId,
        createdAt = createdAt
    )
}
