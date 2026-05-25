package com.mobilemoney.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL AND archived = 0")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE archived = 0")
    fun getAllAccountsIncludingArchived(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND deletedAt IS NULL")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun getDefaultAccount(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE currencyCode = :currencyCode AND deletedAt IS NULL AND archived = 0")
    fun getAccountsByCurrency(currencyCode: String): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("UPDATE accounts SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM accounts WHERE deletedAt IS NOT NULL")
    suspend fun permanentDeleteAll()

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultAccounts()

    @Query("SELECT * FROM accounts WHERE syncedAt IS NULL")
    suspend fun getUnsyncedAccounts(): List<AccountEntity>

    @Query("UPDATE accounts SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT * FROM accounts WHERE cardMask = :mask AND deletedAt IS NULL LIMIT 1")
    suspend fun getAccountByCardMask(mask: String): AccountEntity?
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isIncome = 1 AND deletedAt IS NULL")
    fun getIncomeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isIncome = 0 AND deletedAt IS NULL")
    fun getExpenseCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId IS NULL AND deletedAt IS NULL")
    fun getRootCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId AND deletedAt IS NULL")
    fun getSubcategories(parentId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id AND deletedAt IS NULL")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM categories WHERE deletedAt IS NOT NULL")
    suspend fun permanentDeleteAll()

    @Query("SELECT * FROM categories WHERE syncedAt IS NULL")
    suspend fun getUnsyncedCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isIncome = :isIncome AND isDefault = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun getDefaultCategory(isIncome: Boolean): CategoryEntity?

    @Query("UPDATE categories SET isDefault = 0 WHERE isIncome = :isIncome")
    suspend fun clearDefaultCategories(isIncome: Boolean)

    @Query("UPDATE categories SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND deletedAt IS NULL ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND deletedAt IS NULL ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM transactions WHERE deletedAt IS NOT NULL")
    suspend fun permanentDeleteAll()

    @Query("SELECT * FROM transactions WHERE relatedTransactionId = :relatedId AND id != :excludeId AND deletedAt IS NULL")
    suspend fun getRelatedTransaction(relatedId: String, excludeId: String): TransactionEntity?

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE accountId = :accountId AND deletedAt IS NULL
    """)
    suspend fun getTotalAmountForAccount(accountId: String): Double?

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN c.isIncome = 1 THEN t.amount
                ELSE -t.amount
            END
        ), 0)
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.accountId = :accountId AND t.deletedAt IS NULL
    """)
    suspend fun getAccountBalance(accountId: String): Double?

    @Query("SELECT * FROM transactions WHERE syncedAt IS NULL")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)

    @Query("SELECT t.* FROM transactions t INNER JOIN categories c ON t.categoryId = c.id WHERE t.shop = :shop AND t.deletedAt IS NULL AND c.isIncome = :isIncome ORDER BY t.date DESC LIMIT 1")
    suspend fun getLastTransactionByShop(shop: String, isIncome: Boolean): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE sourceData = :sourceData AND deletedAt IS NULL AND date >= :since")
    suspend fun countBySourceDataSince(sourceData: String, since: Long): Int
}

@Dao
interface SenderDao {
    @Query("SELECT * FROM senders WHERE deletedAt IS NULL ORDER BY label")
    fun getAll(): Flow<List<SenderEntity>>

    @Query("SELECT * FROM senders WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): SenderEntity?

    @Query("SELECT * FROM senders WHERE sender = :senderNumber AND deletedAt IS NULL LIMIT 1")
    suspend fun findBySender(senderNumber: String): SenderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sender: SenderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(senders: List<SenderEntity>)

    @Update
    suspend fun update(sender: SenderEntity)

    @Query("UPDATE senders SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM senders WHERE deletedAt IS NOT NULL")
    suspend fun permanentDeleteAll()

    @Query("SELECT * FROM senders WHERE type = :type AND deletedAt IS NULL")
    suspend fun findByType(type: String): List<SenderEntity>

    @Query("SELECT * FROM senders WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<SenderEntity>

    @Query("UPDATE senders SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY receivedAt DESC")
    fun getAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE processed = 0")
    suspend fun getUnprocessed(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET processed = 1, error = :error, transactionId = :transactionId WHERE id = :id")
    suspend fun markProcessed(id: String, error: String? = null, transactionId: String? = null)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}



@Dao
interface MessageRegexDao {
    @Query("SELECT * FROM message_regexes WHERE deletedAt IS NULL")
    fun getAll(): Flow<List<MessageRegexEntity>>

    @Query("SELECT * FROM message_regexes WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): MessageRegexEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(regex: MessageRegexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(regexes: List<MessageRegexEntity>)

    @Update
    suspend fun update(regex: MessageRegexEntity)

    @Query("UPDATE message_regexes SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM message_regexes WHERE deletedAt IS NOT NULL")
    suspend fun permanentDeleteAll()

    @Query("SELECT * FROM message_regexes WHERE syncedAt IS NULL")
    suspend fun getUnsynced(): List<MessageRegexEntity>

    @Query("UPDATE message_regexes SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}