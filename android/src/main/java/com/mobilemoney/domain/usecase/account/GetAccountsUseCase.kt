package com.mobilemoney.domain.usecase.account

import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> {
        return accountRepository.getAccounts()
    }
}