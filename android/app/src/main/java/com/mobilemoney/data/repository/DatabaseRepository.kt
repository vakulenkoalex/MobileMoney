package com.mobilemoney.data.repository

import android.content.Context
import com.mobilemoney.data.local.AccountDao
import com.mobilemoney.data.local.AppDatabase
import com.mobilemoney.data.local.CategoryDao
import com.mobilemoney.data.local.CurrencyDao
import com.mobilemoney.data.local.CurrencyEntity
import com.mobilemoney.data.local.ExchangeRateDao
import com.mobilemoney.data.local.TagDao
import com.mobilemoney.data.local.TransactionDao
import com.mobilemoney.data.local.AccountEntity
import com.mobilemoney.data.local.CategoryEntity
import com.mobilemoney.data.local.toEntity
import com.mobilemoney.data.local.toUiModel
import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.model.AccountType
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.data.model.TransactionUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class DatabaseRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val currencyDao = database.currencyDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val tagDao = database.tagDao()
    private val transactionDao = database.transactionDao()
    private val exchangeRateDao = database.exchangeRateDao()

    fun getAccounts(): Flow<List<AccountUi>> {
        return combine(
            accountDao.getAllAccounts(),
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories()
        ) { accounts, transactions, categories ->
            val categoriesMap = categories.associateBy { it.id }
            accounts.map { account ->
                val accountTransactions = transactions.filter { it.accountId == account.id }
                val balance = accountTransactions.sumOf { tx ->
                    val category = tx.categoryId?.let { categoriesMap[it] }
                    if (category?.isIncome == true) tx.amount else -tx.amount
                }
                account.toUiModel(balance)
            }
        }
    }

    suspend fun getDefaultAccount(): AccountUi? {
        return accountDao.getDefaultAccount()?.toUiModel()
    }

    fun getCategories(): Flow<List<CategoryUi>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toUiModel() }
        }
    }

    fun getTransactions(): Flow<List<TransactionUi>> {
        return transactionDao.getAllTransactions().map { entities ->
            val transactions = entities.map { entity ->
                entity.toUiModel(
                    accountDao.getAccountById(entity.accountId),
                    categoryDao.getCategoryById(entity.categoryId ?: "")
                )
            }

            val regularTransactions = transactions.filter { it.relatedTransactionId == null }
            val transferTransactions = transactions.filter { it.relatedTransactionId != null }

            val mergedTransfers = mutableListOf<TransactionUi>()
            if (transferTransactions.isNotEmpty()) {
                val groupedTransfers = transferTransactions.groupBy { it.relatedTransactionId }
                for ((_, group) in groupedTransfers) {
                    if (group.size >= 2) {
                        val tx1 = group[0]
                        val tx2 = group[1]
                        val from = if (!tx1.isIncome) tx1.subtitle else tx2.subtitle
                        val to = if (tx1.isIncome) tx1.subtitle else tx2.subtitle
                        mergedTransfers.add(
                            tx1.copy(
                                title = to,
                                subtitle = from,
                                icon = "swap_horiz",
                                color = 0xFF9C27B0,
                                isIncome = true,
                                amount = tx1.amount
                            )
                        )
                    }
                }
            }

            (regularTransactions + mergedTransfers).sortedByDescending { it.date }
        }
    }

    suspend fun getTransactionById(id: String): TransactionUi? {
        val entity = transactionDao.getTransactionById(id) ?: return null
        val account = accountDao.getAccountById(entity.accountId)
        val category = entity.categoryId?.let { categoryDao.getCategoryById(it) }
        return entity.toUiModel(account, category)
    }

    suspend fun getRelatedTransaction(relatedId: String, excludeId: String): TransactionUi? {
        val entity = transactionDao.getRelatedTransaction(relatedId, excludeId) ?: return null
        val account = accountDao.getAccountById(entity.accountId)
        val category = entity.categoryId?.let { categoryDao.getCategoryById(it) }
        return entity.toUiModel(account, category)
    }

    suspend fun addTransaction(transaction: TransactionUi) {
        transactionDao.insert(transaction.toEntity())
    }

    suspend fun updateTransaction(transaction: TransactionUi) {
        transactionDao.update(transaction.toEntity())
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun addAccount(account: AccountUi) {
        accountDao.insert(account.toEntity())
    }

    suspend fun clearDefaultAccounts() {
        accountDao.clearDefaultAccounts()
    }

    suspend fun updateAccount(account: AccountUi) {
        accountDao.update(account.toEntity())
    }

    suspend fun deleteAccount(id: String) {
        accountDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun addCategory(category: CategoryUi) {
        categoryDao.insert(category.toEntity())
    }

    suspend fun updateCategory(category: CategoryUi) {
        categoryDao.update(category.toEntity())
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun initializeDefaultData() {
        initializeCurrencies()
        initializeCategories()
        initializeAccounts()
    }

    private suspend fun initializeAccounts() {
        val defaultAccounts = listOf(
            AccountEntity(
                id = UUID.randomUUID().toString(),
                name = "Наличные",
                typeId = AccountType.CASH.id,
                currencyCode = "RUB",
                icon = "wallet",
                isDefault = true,
                archived = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        accountDao.insertAll(defaultAccounts)
    }

    private suspend fun initializeCurrencies() {
        val defaultCurrencies = listOf(
            CurrencyEntity("RUB", "Российский рубль", "₽", System.currentTimeMillis()),
            CurrencyEntity("USD", "Доллар США", "$", System.currentTimeMillis()),
            CurrencyEntity("EUR", "Евро", "€", System.currentTimeMillis())
        )
        currencyDao.insertAll(defaultCurrencies)
    }

    private suspend fun initializeCategories() {
        val defaultCategories = listOf(
            CategoryEntity(UUID.randomUUID().toString(), "Кафе и рестораны", false, "restaurant", null, System.currentTimeMillis(), System.currentTimeMillis()),
            CategoryEntity(UUID.randomUUID().toString(), "Развлечения", false, "movie", null, System.currentTimeMillis(), System.currentTimeMillis()),
            CategoryEntity(UUID.randomUUID().toString(), "Здоровье", false, "local_hospital", null, System.currentTimeMillis(), System.currentTimeMillis()),
            CategoryEntity(UUID.randomUUID().toString(), "Зарплата", true, "work", null, System.currentTimeMillis(), System.currentTimeMillis()),
            CategoryEntity(UUID.randomUUID().toString(), "Подарок", true, "card_giftcard", null, System.currentTimeMillis(), System.currentTimeMillis()),
            CategoryEntity(UUID.randomUUID().toString(), "Корректировка", false, "more_horiz", null, System.currentTimeMillis(), System.currentTimeMillis()),
            CategoryEntity(UUID.randomUUID().toString(), "Корректировка", true, "more_horiz", null, System.currentTimeMillis(), System.currentTimeMillis())
        )
        categoryDao.insertAll(defaultCategories)
    }

    suspend fun permanentlyDeleteAll() {
        transactionDao.permanentDeleteAll()
        accountDao.permanentDeleteAll()
        categoryDao.permanentDeleteAll()
        tagDao.permanentDeleteAll()
    }
}