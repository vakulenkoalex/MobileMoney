package com.mobilemoney.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionSource {
    MANUAL,
    SMS,
    PUSH
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey val code: String,
    val name: String,
    val symbol: String,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["currencyCode"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("currencyCode")]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val typeId: String,
    val currencyCode: String?,
    val icon: String,
    val isDefault: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("parentId")]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isIncome: Boolean,
    val icon: String,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("creatorId"),
        Index("relatedTransactionId")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String?,
    val amount: Double,
    val date: Long,
    val comment: String,
    val source: TransactionSource = TransactionSource.MANUAL,
    val sourceData: String?,
    val creatorId: String?,
    val relatedTransactionId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null
)

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId"), Index("tagId")]
)
data class TransactionTagCrossRef(
    val transactionId: String,
    val tagId: String
)

@Entity(
    tableName = "category_tags",
    primaryKeys = ["categoryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("tagId")]
)
data class CategoryTagCrossRef(
    val categoryId: String,
    val tagId: String
)

@Entity(
    tableName = "exchange_rates",
    primaryKeys = ["currencyFrom", "currencyTo", "date"],
    foreignKeys = [
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["currencyFrom"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CurrencyEntity::class,
            parentColumns = ["code"],
            childColumns = ["currencyTo"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("currencyFrom"), Index("currencyTo")]
)
data class ExchangeRateEntity(
    val currencyFrom: String,
    val currencyTo: String,
    val rate: Double,
    val date: Long
)