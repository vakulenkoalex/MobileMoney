package com.mobilemoney.server.repository

import com.mobilemoney.dto.SenderDto

class SenderRepository {
    fun upsert(data: SenderDto) {
        val serverReceivedAt = System.currentTimeMillis()
        Database.getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT OR REPLACE INTO senders (id, sender, label, type, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, data.id)
                stmt.setString(2, data.sender)
                stmt.setString(3, data.label)
                stmt.setString(4, data.type)
                stmt.setLong(5, data.createdAt)
                stmt.setLong(6, data.updatedAt)
                if (data.deletedAt != null) stmt.setString(7, data.deletedAt.toString()) else stmt.setNull(7, java.sql.Types.VARCHAR)
                if (data.syncedAt != null) stmt.setString(8, data.syncedAt.toString()) else stmt.setNull(8, java.sql.Types.VARCHAR)
                stmt.setLong(9, serverReceivedAt)
                stmt.executeUpdate()
            }
        }
    }

    fun getAll(): List<SenderDto> {
        val result = mutableListOf<SenderDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM senders WHERE deletedAt IS NULL").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) result.add(mapRow(rs))
                }
            }
        }
        return result
    }

    fun getUpdatedSince(since: Long): List<SenderDto> {
        val result = mutableListOf<SenderDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM senders WHERE serverReceivedAt > ?").use { stmt ->
                stmt.setLong(1, since)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) result.add(mapRow(rs))
                }
            }
        }
        return result
    }

    private fun mapRow(rs: java.sql.ResultSet): SenderDto {
        return SenderDto(
            id = rs.getString("id"),
            sender = rs.getString("sender"),
            label = rs.getString("label"),
            type = rs.getString("type") ?: "",
            createdAt = rs.getLong("createdAt"),
            updatedAt = rs.getLong("updatedAt"),
            deletedAt = rs.getString("deletedAt")?.toLongOrNull(),
            syncedAt = rs.getLong("syncedAt").takeIf { it > 0 },
            serverReceivedAt = rs.getLong("serverReceivedAt").takeIf { it > 0 }
        )
    }
}
