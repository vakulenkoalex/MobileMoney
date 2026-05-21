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

    @Query("SELECT * FROM transactions WHERE shop = :shop AND deletedAt IS NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLastTransactionByShop(shop: String): TransactionEntity?
}