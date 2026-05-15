package com.mobilemoney.server.service

import com.mobilemoney.server.model.dto.SyncChangesResponse
import com.mobilemoney.server.model.dto.SyncPushRequestDto
import com.mobilemoney.server.model.dto.SyncResponse
import com.mobilemoney.server.repository.AccountRepository
import com.mobilemoney.server.repository.CategoryRepository
import com.mobilemoney.server.repository.TransactionRepository

class SyncService(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {

    fun push(data: SyncPushRequestDto): SyncResponse {
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

        println("Push: $syncedCount items")
        return SyncResponse(success = true, timestamp = System.currentTimeMillis(), synced = syncedCount)
    }

    fun getChanges(since: Long): SyncChangesResponse {
        return SyncChangesResponse(
            timestamp = System.currentTimeMillis(),
            accounts = accountRepository.getUpdatedSince(since),
            categories = categoryRepository.getUpdatedSince(since),
            transactions = transactionRepository.getUpdatedSince(since)
        )
    }

    fun pull(): SyncChangesResponse {
        return SyncChangesResponse(
            timestamp = System.currentTimeMillis(),
            accounts = accountRepository.getAll(),
            categories = categoryRepository.getAll(),
            transactions = transactionRepository.getAll()
        )
    }
}