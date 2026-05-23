package com.mobilemoney.server.service

import com.mobilemoney.dto.SyncChangesResponse
import com.mobilemoney.dto.SyncPushRequest
import com.mobilemoney.dto.SyncPushResponse
import com.mobilemoney.server.repository.AccountRepository
import com.mobilemoney.server.repository.CategoryRepository
import com.mobilemoney.server.repository.MessageRegexRepository
import com.mobilemoney.server.repository.SenderRepository
import com.mobilemoney.server.repository.TransactionRepository

class SyncService(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val messageRegexRepository: MessageRegexRepository,
    private val senderRepository: SenderRepository
) {

    fun push(data: SyncPushRequest): SyncPushResponse {
        var syncedCount = 0

        data.accounts.forEach {
            accountRepository.upsert(it)
            syncedCount++
        }

        data.categories.forEach {
            categoryRepository.upsert(it)
            syncedCount++
        }

        data.transactions.forEach {
            transactionRepository.upsert(it)
            syncedCount++
        }

        data.messageRegexes.forEach {
            messageRegexRepository.upsert(it)
            syncedCount++
        }

        data.senders.forEach {
            senderRepository.upsert(it)
            syncedCount++
        }

        println("Push: $syncedCount items")
        return SyncPushResponse(success = true, timestamp = System.currentTimeMillis(), synced = syncedCount)
    }

    fun getChanges(since: Long): SyncChangesResponse {
        return SyncChangesResponse(
            timestamp = System.currentTimeMillis(),
            accounts = accountRepository.getUpdatedSince(since),
            categories = categoryRepository.getUpdatedSince(since),
            transactions = transactionRepository.getUpdatedSince(since),
            messageRegexes = messageRegexRepository.getUpdatedSince(since),
            senders = senderRepository.getUpdatedSince(since)
        )
    }

    fun pull(): SyncChangesResponse {
        return SyncChangesResponse(
            timestamp = System.currentTimeMillis(),
            accounts = accountRepository.getAll(),
            categories = categoryRepository.getAll(),
            transactions = transactionRepository.getAll(),
            messageRegexes = messageRegexRepository.getAll(),
            senders = senderRepository.getAll()
        )
    }
}