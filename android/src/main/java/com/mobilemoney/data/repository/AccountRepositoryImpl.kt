package com.mobilemoney.data.repository

import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.model.AccountType as DataAccountType
import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.model.AccountType
import com.mobilemoney.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val databaseRepository: DatabaseRepository
) : AccountRepository {

    override fun getAccounts(): Flow<List<Account>> {
        return databaseRepository.getAccounts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(id: String): Account? {
        return getAccounts().first().find { it.id.toString() == id }
    }

    override suspend fun getDefaultAccount(): Account? {
        return databaseRepository.getDefaultAccount()?.toDomain()
    }

    override suspend fun getAccountByCardMask(cardMask: String): Account? {
        return databaseRepository.getAccountByCardMask(cardMask)?.toDomain()
    }

    override suspend fun getAccountBalance(accountId: String): Double {
        return databaseRepository.getAccountBalanceValue(accountId)
    }

    override suspend fun addAccount(account: Account) {
        databaseRepository.addAccount(account.toUiModel())
    }

    override suspend fun updateAccount(account: Account) {
        databaseRepository.updateAccount(account.toUiModel())
    }

    override suspend fun deleteAccount(id: String) {
        databaseRepository.deleteAccount(id)
    }

    override suspend fun clearDefaultAccounts() {
        databaseRepository.clearDefaultAccounts()
    }
}

private fun AccountUi.toDomain(): Account {
    val domainType = try {
        AccountType.entries.find { it.id == type.id } ?: AccountType.CASH
    } catch (e: Exception) {
        AccountType.CASH
    }
    return Account(
        id = id,
        name = name,
        type = domainType,
        currency = currency,
        icon = icon,
        isDefault = isDefault,
        balance = balance,
        autoCreateEnabled = autoCreateEnabled,
        cardMask = cardMask
    )
}

private fun Account.toUiModel(): AccountUi {
    val dataType = when (type) {
        AccountType.CASH -> DataAccountType.CASH
        AccountType.CARD -> DataAccountType.CARD
        AccountType.ACCOUNT -> DataAccountType.ACCOUNT
    }
    return AccountUi(
        id = id,
        name = name,
        type = dataType,
        currency = currency,
        icon = icon,
        isDefault = isDefault,
        autoCreateEnabled = autoCreateEnabled,
        cardMask = cardMask
    )
}