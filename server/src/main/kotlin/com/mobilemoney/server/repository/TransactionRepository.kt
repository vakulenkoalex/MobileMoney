package com.mobilemoney.server.repository

import com.mobilemoney.dto.TransactionDto

class TransactionRepository {

    fun upsert(data: TransactionDto) {
        val serverReceivedAt = System.currentTimeMillis()
        Database.getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT OR REPLACE INTO transactions (id, accountId, categoryId, amount, date, comment, source, sourceData, creatorId, relatedTransactionId, shop, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, data.id)
                stmt.setString(2, data.accountId)
                stmt.setString(3, data.categoryId)
                stmt.setDouble(4, data.amount)
                stmt.setLong(5, data.date)
                stmt.setString(6, data.comment)
                stmt.setString(7, data.source)
                stmt.setString(8, data.sourceData)
                stmt.setString(9, data.creatorId)
                stmt.setString(10, data.relatedTransactionId)
                stmt.setString(11, data.shop)
                stmt.setLong(12, data.createdAt)
                stmt.setLong(13, data.updatedAt)
                stmt.setString(14, data.deletedAt?.toString())
                stmt.setString(15, data.syncedAt?.toString())
                stmt.setLong(16, serverReceivedAt)
                stmt.executeUpdate()
            }
        }
    }

    fun getAll(): List<TransactionDto> {
        val result = mutableListOf<TransactionDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM transactions WHERE deletedAt IS NULL").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        result.add(mapRow(rs))
                    }
                }
            }
        }
        return result
    }

    fun getUpdatedSince(since: Long): List<TransactionDto> {
        val result = mutableListOf<TransactionDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM transactions WHERE serverReceivedAt > ?").use { stmt ->
                stmt.setLong(1, since)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        result.add(mapRow(rs))
                    }
                }
            }
        }
        return result
    }

    private fun mapRow(rs: java.sql.ResultSet): TransactionDto {
        return TransactionDto(
            id = rs.getString("id"),
            accountId = rs.getString("accountId"),
            categoryId = rs.getString("categoryId"),
            amount = rs.getDouble("amount"),
            date = rs.getLong("date"),
            comment = rs.getString("comment"),
            source = rs.getString("source") ?: "manual",
            sourceData = rs.getString("sourceData")?.takeIf { it.isNotEmpty() },
            creatorId = rs.getString("creatorId")?.takeIf { it.isNotEmpty() },
            relatedTransactionId = rs.getString("relatedTransactionId")?.takeIf { it.isNotEmpty() },
            shop = rs.getString("shop"),
            createdAt = rs.getLong("createdAt"),
            updatedAt = rs.getLong("updatedAt"),
            deletedAt = rs.getString("deletedAt")?.toLongOrNull(),
            syncedAt = rs.getLong("syncedAt").takeIf { it > 0 },
            serverReceivedAt = rs.getLong("serverReceivedAt").takeIf { it > 0 }
        )
    }
}