package com.mobilemoney.di

import android.content.Context
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.repository.*
import com.mobilemoney.domain.repository.AccountRepository
import com.mobilemoney.domain.repository.CategoryRepository
import com.mobilemoney.domain.repository.TransactionRepository
import com.mobilemoney.domain.repository.MessageRegexRepository
import com.mobilemoney.domain.usecase.account.CreateAccountUseCase
import com.mobilemoney.domain.usecase.account.GetAccountsUseCase
import com.mobilemoney.domain.usecase.category.GetCategoriesUseCase
import com.mobilemoney.domain.usecase.transaction.DeleteTransactionUseCase
import com.mobilemoney.domain.usecase.transaction.GetTransactionsUseCase
import com.mobilemoney.domain.usecase.transaction.SaveTransactionUseCase
import com.mobilemoney.domain.usecase.clipboard.DeleteMessageRegexUseCase
import com.mobilemoney.domain.usecase.clipboard.GetMessageRegexesUseCase
import com.mobilemoney.domain.usecase.clipboard.SaveMessageRegexUseCase
import com.mobilemoney.viewmodel.*

object DI {
    val context: Context
        get() = MobileMoneyApp.instance

    val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }

    private val accountDao by lazy { database.accountDao() }
    private val categoryDao by lazy { database.categoryDao() }
    private val transactionDao by lazy { database.transactionDao() }
    private val messageRegexDao by lazy { database.messageRegexDao() }

    val databaseRepository: DatabaseRepository by lazy {
        DatabaseRepository(accountDao, categoryDao, transactionDao)
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(databaseRepository)
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(databaseRepository)
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(databaseRepository)
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository.getInstance(context)
    }

    val clipboardPreferences: ClipboardPreferences by lazy {
        ClipboardPreferences(context)
    }

    val messageRegexRepository: MessageRegexRepository by lazy {
        MessageRegexRepositoryImpl(messageRegexDao)
    }

    val getAccountsUseCase: GetAccountsUseCase by lazy {
        GetAccountsUseCase(accountRepository)
    }

    val createAccountUseCase: CreateAccountUseCase by lazy {
        CreateAccountUseCase(accountRepository)
    }

    val getCategoriesUseCase: GetCategoriesUseCase by lazy {
        GetCategoriesUseCase(categoryRepository)
    }

    val getTransactionsUseCase: GetTransactionsUseCase by lazy {
        GetTransactionsUseCase(transactionRepository)
    }

    val saveTransactionUseCase: SaveTransactionUseCase by lazy {
        SaveTransactionUseCase(transactionRepository)
    }

    val deleteTransactionUseCase: DeleteTransactionUseCase by lazy {
        DeleteTransactionUseCase(transactionRepository)
    }

    val getMessageRegexesUseCase: GetMessageRegexesUseCase by lazy {
        GetMessageRegexesUseCase(messageRegexRepository)
    }

    val saveMessageRegexUseCase: SaveMessageRegexUseCase by lazy {
        SaveMessageRegexUseCase(messageRegexRepository)
    }

    val deleteMessageRegexUseCase: DeleteMessageRegexUseCase by lazy {
        DeleteMessageRegexUseCase(messageRegexRepository)
    }

    val accountListViewModel: AccountListViewModel by lazy {
        AccountListViewModel(getAccountsUseCase)
    }

    val accountFormViewModel: AccountFormViewModel by lazy {
        AccountFormViewModel(getAccountsUseCase, createAccountUseCase)
    }

    val categoryListViewModel: CategoryListViewModel by lazy {
        CategoryListViewModel(getCategoriesUseCase)
    }

    val transactionListViewModel: TransactionListViewModel by lazy {
        TransactionListViewModel(getTransactionsUseCase)
    }

    val transactionFormViewModel: TransactionFormViewModel by lazy {
        TransactionFormViewModel(
            getAccountsUseCase,
            getCategoriesUseCase,
            getTransactionsUseCase,
            saveTransactionUseCase,
            deleteTransactionUseCase,
            transactionRepository
        )
    }

    val categoryFormViewModel: CategoryFormViewModel by lazy {
        CategoryFormViewModel(categoryRepository)
    }

    val settingsViewModel: SettingsViewModel by lazy {
        SettingsViewModel(syncRepository, BackupRepository(context))
    }

    val messageRegexListViewModel: MessageRegexListViewModel by lazy {
        MessageRegexListViewModel(getMessageRegexesUseCase, deleteMessageRegexUseCase)
    }

    val messageRegexFormViewModel: MessageRegexFormViewModel by lazy {
        MessageRegexFormViewModel(getMessageRegexesUseCase, saveMessageRegexUseCase)
    }
}