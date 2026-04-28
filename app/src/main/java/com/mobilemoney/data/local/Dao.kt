package com.mobilemoney.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE deletedAt IS NULL")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id AND deletedAt IS NULL")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email AND deletedAt IS NULL")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE users SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)
}

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies WHERE deletedAt IS NULL")
    fun getAllCurrencies(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE code = :code AND deletedAt IS NULL")
    suspend fun getCurrencyByCode(code: String): CurrencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(currency: CurrencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<CurrencyEntity>)

    @Update
    suspend fun update(currency: CurrencyEntity)

    @Query("UPDATE currencies SET deletedAt = :deletedAt WHERE code = :code")
    suspend fun softDelete(code: String, deletedAt: Long)
}

@Dao
interface AccountTypeDao {
    @Query("SELECT * FROM account_types WHERE deletedAt IS NULL")
    fun getAllAccountTypes(): Flow<List<AccountTypeEntity>>

    @Query("SELECT * FROM account_types WHERE id = :id AND deletedAt IS NULL")
    suspend fun getAccountTypeById(id: String): AccountTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accountType: AccountTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accountTypes: List<AccountTypeEntity>)

    @Update
    suspend fun update(accountType: AccountTypeEntity)

    @Query("UPDATE account_types SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE deletedAt IS NULL AND archived = 0")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE archived = 0")
    fun getAllAccountsIncludingArchived(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND deletedAt IS NULL")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE typeId = :typeId AND deletedAt IS NULL AND archived = 0")
    fun getAccountsByType(typeId: String): Flow<List<AccountEntity>>

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
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL")
    fun getAllCategories(): Flow<List<CategoryEntity>>

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
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE deletedAt IS NULL")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id AND deletedAt IS NULL")
    suspend fun getTagById(id: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("UPDATE tags SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN transaction_tags tt ON t.id = tt.tagId
        WHERE tt.transactionId = :transactionId AND t.deletedAt IS NULL
    """)
    fun getTagsForTransaction(transactionId: String): Flow<List<TagEntity>>
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

    @Query("SELECT * FROM transactions WHERE relatedTransactionId = :relatedId AND deletedAt IS NULL")
    suspend fun getRelatedTransaction(relatedId: String): TransactionEntity?

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE accountId = :accountId AND deletedAt IS NULL
    """)
    suspend fun getTotalAmountForAccount(accountId: String): Double?
}

@Dao
interface TransactionTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: TransactionTagCrossRef)

    @Delete
    suspend fun delete(crossRef: TransactionTagCrossRef)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteAllForTransaction(transactionId: String)

    @Query("SELECT * FROM transaction_tags WHERE transactionId = :transactionId")
    fun getTagsForTransaction(transactionId: String): Flow<List<TransactionTagCrossRef>>
}

@Dao
interface CategoryTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: CategoryTagCrossRef)

    @Delete
    suspend fun delete(crossRef: CategoryTagCrossRef)

    @Query("DELETE FROM category_tags WHERE categoryId = :categoryId")
    suspend fun deleteAllForCategory(categoryId: String)

    @Query("SELECT * FROM category_tags WHERE categoryId = :categoryId")
    fun getTagsForCategory(categoryId: String): Flow<List<CategoryTagCrossRef>>
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE currencyFrom = :from AND currencyTo = :to")
    fun getExchangeRates(from: String, to: String): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE currencyFrom = :from AND currencyTo = :to AND date = :date")
    suspend fun getExchangeRate(from: String, to: String, date: Long): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exchangeRate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exchangeRates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates WHERE date < :date")
    suspend fun deleteOldRates(date: Long)
}