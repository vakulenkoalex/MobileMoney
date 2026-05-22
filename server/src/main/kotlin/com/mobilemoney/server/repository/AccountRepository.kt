package com.mobilemoney.server.repository

import com.mobilemoney.dto.AccountDto

class AccountRepository {

    fun upsert(data: AccountDto) {
        val serverReceivedAt = System.currentTimeMillis()
        Database.getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT OR REPLACE INTO accounts (id, name, typeId, currencyCode, icon, isDefault, archived, autoCreateEnabled, cardMask, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, data.id)
                stmt.setString(2, data.name)
                stmt.setString(3, data.typeId)
                stmt.setString(4, data.currencyCode ?: "")
                stmt.setString(5, data.icon)
                stmt.setInt(6, if (data.isDefault) 1 else 0)
                stmt.setInt(7, if (data.archived) 1 else 0)
                stmt.setInt(8, if (data.autoCreateEnabled) 1 else 0)
                stmt.setString(9, data.cardMask)
                stmt.setLong(10, data.createdAt)
                stmt.setLong(11, data.updatedAt)
                stmt.setString(12, data.deletedAt?.toString())
                stmt.setString(13, data.syncedAt?.toString())
                stmt.setLong(14, serverReceivedAt)
                stmt.executeUpdate()
            }
        }
    }

    fun getAll(): List<AccountDto> {
        val result = mutableListOf<AccountDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM accounts WHERE deletedAt IS NULL").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        result.add(mapRow(rs))
                    }
                }
            }
        }
        return result
    }

    fun getUpdatedSince(since: Long): List<AccountDto> {
        val result = mutableListOf<AccountDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM accounts WHERE serverReceivedAt > ?").use { stmt ->
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

    private fun mapRow(rs: java.sql.ResultSet): AccountDto {
        return AccountDto(
            id = rs.getString("id"),
            name = rs.getString("name"),
            typeId = rs.getString("typeId"),
            currencyCode = rs.getString("currencyCode"),
            icon = rs.getString("icon") ?: "",
            isDefault = rs.getInt("isDefault") == 1,
            archived = rs.getInt("archived") == 1,
            autoCreateEnabled = rs.getInt("autoCreateEnabled") == 1,
            cardMask = rs.getString("cardMask"),
            createdAt = rs.getLong("createdAt"),
            updatedAt = rs.getLong("updatedAt"),
            deletedAt = rs.getString("deletedAt")?.toLongOrNull(),
            syncedAt = rs.getLong("syncedAt").takeIf { it > 0 },
            serverReceivedAt = rs.getLong("serverReceivedAt").takeIf { it > 0 }
        )
    }
}