package com.mobilemoney.data.repository

import androidx.room.Transaction
import com.mobilemoney.data.local.AccountDao
import com.mobilemoney.data.local.CategoryDao
import com.mobilemoney.data.local.MessageDao
import com.mobilemoney.data.local.MessageEntity
import com.mobilemoney.data.local.SenderDao
import com.mobilemoney.data.local.SenderEntity
import com.mobilemoney.data.local.TransactionDao
import com.mobilemoney.data.local.toEntity
import com.mobilemoney.data.local.toUiModel
import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.domain.helper.BalanceCalculator
import com.mobilemoney.domain.helper.TransferMerger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DatabaseRepository(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val messageDao: MessageDao,
    private val senderDao: SenderDao
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
                val balance = BalanceCalculator.calculateBalance(
                    transactions = accountTransactions.map { tx ->
                        BalanceCalculator.TransactionBalanceData(
                            amount = tx.amount,
                            categoryId = tx.categoryId
                        )
                    },
                    getIsIncome = { categoryId ->
                        categoryId?.let { categoriesMap[it]?.isIncome } ?: false
                    }
                )
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

            val mergedInfo = TransferMerger.identifyMergedTransfers(
                transactions.map { tx ->
                    TransferMerger.TransactionInfo(
                        relatedTransactionId = tx.relatedTransactionId?.toString(),
                        isIncome = tx.isIncome,
                        subtitle = tx.subtitle,
                        amount = tx.amount
                    )
                }
            )

            val mergedIds = mergedInfo.map { it.relatedTransactionId }.toSet()
            val regularTransactions = transactions.filter { tx ->
                val rid = tx.relatedTransactionId?.toString()
                rid == null || rid !in mergedIds
            }

            val mergedTransfers = mergedInfo.map { info ->
                val tx = transactions.find { tx ->
                    tx.relatedTransactionId?.toString() == info.relatedTransactionId && !tx.isIncome
                } ?: transactions.first { tx ->
                    tx.relatedTransactionId?.toString() == info.relatedTransactionId
                }
                tx.copy(
                    title = info.title,
                    subtitle = info.subtitle,
                    icon = "swap_horiz",
                    color = 0xFF9C27B0,
                    isIncome = true,
                    amount = tx.amount
                )
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

    suspend fun getDefaultCategory(isIncome: Boolean): CategoryUi? {
        return categoryDao.getDefaultCategory(isIncome)?.toUiModel()
    }

    suspend fun clearDefaultCategories(isIncome: Boolean) {
        categoryDao.clearDefaultCategories(isIncome)
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

    suspend fun getLastTransactionByShop(shop: String, isIncome: Boolean = false): TransactionUi? {
        val entity = transactionDao.getLastTransactionByShop(shop, isIncome) ?: return null
        val account = accountDao.getAccountById(entity.accountId)
        val category = entity.categoryId?.let { categoryDao.getCategoryById(it) }
        return entity.toUiModel(account, category)
    }

    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insert(message)
    }

    fun getMessages(): Flow<List<MessageEntity>> {
        return messageDao.getAll()
    }

    suspend fun getUnprocessedMessages(): List<MessageEntity> {
        return messageDao.getUnprocessed()
    }

    suspend fun markMessageProcessed(id: String, error: String? = null, transactionId: String? = null) {
        messageDao.markProcessed(id, error, transactionId)
    }

    suspend fun deleteMessageById(id: String) {
        messageDao.deleteById(id)
    }

    suspend fun deleteAllMessages() {
        messageDao.deleteAll()
    }

    fun getSenders(): Flow<List<SenderEntity>> {
        return senderDao.getAll()
    }

    suspend fun getSenderById(id: String): SenderEntity? {
        return senderDao.getById(id)
    }

    suspend fun addSender(sender: SenderEntity) {
        senderDao.insert(sender)
    }

    suspend fun updateSender(sender: SenderEntity) {
        senderDao.update(sender)
    }

    suspend fun deleteSender(id: String) {
        senderDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun findSenderByNumber(sender: String): SenderEntity? {
        return senderDao.findBySender(sender)
    }

    suspend fun getAccountByCardMask(mask: String): AccountUi? {
        return accountDao.getAccountByCardMask(mask)?.toUiModel()
    }

    suspend fun transactionExistsBySourceData(sourceData: String): Boolean {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return transactionDao.countBySourceDataSince(sourceData, todayStart) > 0
    }

    suspend fun getAccountBalanceValue(accountId: String): Double {
        return transactionDao.getAccountBalance(accountId) ?: 0.0
    }
}