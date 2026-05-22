package com.mobilemoney.server.repository

import com.mobilemoney.dto.MessageRegexDto

class MessageRegexRepository {

    fun upsert(data: MessageRegexDto) {
        val serverReceivedAt = System.currentTimeMillis()
        Database.getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT OR REPLACE INTO message_regexes (id, pattern, skipBalanceCheck, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, data.id)
                stmt.setString(2, data.pattern)
                stmt.setInt(3, if (data.skipBalanceCheck) 1 else 0)
                stmt.setLong(4, data.createdAt)
                stmt.setLong(5, data.updatedAt)
                stmt.setString(6, data.deletedAt?.toString())
                stmt.setString(7, data.syncedAt?.toString())
                stmt.setLong(8, serverReceivedAt)
                stmt.executeUpdate()
            }
        }
    }

    fun getAll(): List<MessageRegexDto> {
        val result = mutableListOf<MessageRegexDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM message_regexes WHERE deletedAt IS NULL").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        result.add(mapRow(rs))
                    }
                }
            }
        }
        return result
    }

    fun getUpdatedSince(since: Long): List<MessageRegexDto> {
        val result = mutableListOf<MessageRegexDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM message_regexes WHERE serverReceivedAt > ?").use { stmt ->
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

    private fun mapRow(rs: java.sql.ResultSet): MessageRegexDto {
        return MessageRegexDto(
            id = rs.getString("id"),
            pattern = rs.getString("pattern"),
            skipBalanceCheck = rs.getInt("skipBalanceCheck") == 1,
            createdAt = rs.getLong("createdAt"),
            updatedAt = rs.getLong("updatedAt"),
            deletedAt = rs.getString("deletedAt")?.toLongOrNull(),
            syncedAt = rs.getLong("syncedAt").takeIf { it > 0 },
            serverReceivedAt = rs.getLong("serverReceivedAt").takeIf { it > 0 }
        )
    }
}
