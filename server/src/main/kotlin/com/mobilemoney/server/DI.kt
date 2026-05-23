package com.mobilemoney.server

import com.mobilemoney.server.repository.AccountRepository
import com.mobilemoney.server.repository.CategoryRepository
import com.mobilemoney.server.repository.DeviceRepository
import com.mobilemoney.server.repository.MessageRegexRepository
import com.mobilemoney.server.repository.SenderRepository
import com.mobilemoney.server.repository.TransactionRepository
import com.mobilemoney.server.repository.UserRepository
import com.mobilemoney.server.service.AuthService
import com.mobilemoney.server.service.SyncService

object DI {
    val userRepository = UserRepository()
    val deviceRepository = DeviceRepository()
    val accountRepository = AccountRepository()
    val categoryRepository = CategoryRepository()
    val transactionRepository = TransactionRepository()
    val messageRegexRepository = MessageRegexRepository()
    val senderRepository = SenderRepository()

    val authService = AuthService(userRepository, deviceRepository)
    val syncService = SyncService(accountRepository, categoryRepository, transactionRepository, messageRegexRepository, senderRepository)
}