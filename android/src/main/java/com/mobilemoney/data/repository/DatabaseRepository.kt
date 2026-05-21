package com.mobilemoney.data.repository

import androidx.room.Transaction
import com.mobilemoney.data.local.AccountDao
import com.mobilemoney.data.local.CategoryDao
import com.mobilemoney.data.local.TransactionDao
import com.mobilemoney.data.local.toEntity
import com.mobilemoney.data.local.toUiModel
import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.data.model.TransactionUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DatabaseRepository(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {

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
        transactionDao.insert(transaction.toEntity(source = transaction.source, sourceData = transaction.sourceData))
    }

    suspend fun updateTransaction(transaction: TransactionUi) {
        transactionDao.update(transaction.toEntity(source = transaction.source, sourceData = transaction.sourceData))
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.softDelete(id, System.currentTimeMillis())
    }

    @Transaction
    suspend fun splitTransaction(
        originalId: String,
        mainAmount: Double,
        newTransaction: TransactionUi
    ) {
        val entity = transactionDao.getTransactionById(originalId)
        if (entity != null) {
            transactionDao.update(entity.copy(amount = mainAmount))
        }
        transactionDao.insert(newTransaction.toEntity(source = newTransaction.source, sourceData = newTransaction.sourceData))
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

    suspend fun permanentlyDeleteAll() {
        transactionDao.permanentDeleteAll()
        accountDao.permanentDeleteAll()
        categoryDao.permanentDeleteAll()
    }

    suspend fun getLastTransactionByShop(shop: String): TransactionUi? {
        val entity = transactionDao.getLastTransactionByShop(shop) ?: return null
        val account = accountDao.getAccountById(entity.accountId)
        val category = entity.categoryId?.let { categoryDao.getCategoryById(it) }
        return entity.toUiModel(account, category)
    }
}