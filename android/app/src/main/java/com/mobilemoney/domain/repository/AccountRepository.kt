package com.mobilemoney.domain.repository

import com.mobilemoney.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: String): Account?
    suspend fun getDefaultAccount(): Account?
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: String)
    suspend fun clearDefaultAccounts()
}