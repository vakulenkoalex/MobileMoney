package com.mobilemoney.domain.usecase.account

import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.repository.AccountRepository

class CreateAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account, clearDefault: Boolean = false) {
        if (clearDefault) {
            accountRepository.clearDefaultAccounts()
        }
        accountRepository.addAccount(account)
    }
}