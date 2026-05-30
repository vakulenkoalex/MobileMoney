package com.mobilemoney.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.mobilemoney.domain.repository.MessageRepository
import com.mobilemoney.domain.usecase.transaction.ProcessSmsTransactionUseCase
import com.mobilemoney.worker.MessageWorker

class MobileMoneyWorkerFactory(
    private val processSmsTransactionUseCase: ProcessSmsTransactionUseCase,
    private val messageRepository: MessageRepository
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            MessageWorker::class.java.name -> MessageWorker(
                appContext,
                workerParameters,
                processSmsTransactionUseCase,
                messageRepository
            )
            else -> null
        }
    }
}
