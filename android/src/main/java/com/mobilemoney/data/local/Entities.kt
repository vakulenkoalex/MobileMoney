package com.mobilemoney.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionSource {
    MANUAL,
    SMS,
    PUSH,
    CLIPBOARD
}

@Entity(
    tableName = "message_regexes"
)
data class MessageRegexEntity(
    @PrimaryKey val id: String,
    val pattern: String,
    val skipBalanceCheck: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)

@Entity(
    tableName = "accounts",
    indices = [Index("currencyCode")]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val typeId: String,
    val currencyCode: String,
    val icon: String,
    val isDefault: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null,
    val autoCreateEnabled: Boolean = false,
    val cardMask: String? = null
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
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)

@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("relatedTransactionId")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String,
    val amount: Double,
    val date: Long,
    val comment: String,
    val source: TransactionSource = TransactionSource.MANUAL,
    val sourceData: String?,
    val creatorId: String?,
    val relatedTransactionId: String?,
    val shop: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)

@Entity(
    tableName = "senders",
    indices = [Index("sender", unique = true)]
)
data class SenderEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val label: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val processed: Boolean = false,
    val error: String? = null,
    val transactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)