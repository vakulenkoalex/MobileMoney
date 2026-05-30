package com.mobilemoney.data.repository

import com.mobilemoney.data.local.SenderEntity
import com.mobilemoney.domain.model.Sender
import com.mobilemoney.domain.model.SenderType
import com.mobilemoney.domain.repository.SenderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SenderRepositoryImpl(
    private val databaseRepository: DatabaseRepository
) : SenderRepository {

    override fun getSenders(): Flow<List<Sender>> {
        return databaseRepository.getSenders().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getSenderById(id: String): Sender? {
        return databaseRepository.getSenderById(id)?.toDomain()
    }

    override suspend fun addSender(sender: String, label: String, type: SenderType) {
        val now = System.currentTimeMillis()
        val entity = SenderEntity(
            id = UUID.randomUUID().toString(),
            sender = sender,
            label = label,
            type = type.name,
            createdAt = now,
            updatedAt = now
        )
        databaseRepository.addSender(entity)
    }

    override suspend fun updateSender(id: String, sender: String, label: String, type: SenderType) {
        val now = System.currentTimeMillis()
        val existing = databaseRepository.getSenderById(id)
        val entity = SenderEntity(
            id = id,
            sender = sender,
            label = label,
            type = type.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        databaseRepository.updateSender(entity)
    }

    override suspend fun deleteSender(id: String) {
        databaseRepository.deleteSender(id)
    }
}

private fun SenderEntity.toDomain(): Sender {
    val domainType = try {
        SenderType.valueOf(type)
    } catch (e: Exception) {
        SenderType.PHONE_NUMBER
    }
    return Sender(
        id = id,
        sender = sender,
        label = label,
        type = domainType
    )
}
