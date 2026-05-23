package com.mobilemoney.server.repository

import com.mobilemoney.dto.CategoryDto

class CategoryRepository {

    fun upsert(data: CategoryDto) {
        val serverReceivedAt = System.currentTimeMillis()
        Database.getConnection().use { conn ->
            conn.prepareStatement("""
                INSERT OR REPLACE INTO categories (id, name, isIncome, icon, parentId, isDefault, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, data.id)
                stmt.setString(2, data.name)
                stmt.setInt(3, if (data.isIncome) 1 else 0)
                stmt.setString(4, data.icon)
                stmt.setString(5, data.parentId)
                stmt.setInt(6, if (data.isDefault) 1 else 0)
                stmt.setLong(7, data.createdAt)
                stmt.setLong(8, data.updatedAt)
                stmt.setString(9, data.deletedAt?.toString())
                stmt.setString(10, data.syncedAt?.toString())
                stmt.setLong(11, serverReceivedAt)
                stmt.executeUpdate()
            }
        }
    }

    fun getAll(): List<CategoryDto> {
        val result = mutableListOf<CategoryDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM categories WHERE deletedAt IS NULL").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        result.add(mapRow(rs))
                    }
                }
            }
        }
        return result
    }

    fun getUpdatedSince(since: Long): List<CategoryDto> {
        val result = mutableListOf<CategoryDto>()
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT * FROM categories WHERE serverReceivedAt > ?").use { stmt ->
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

    private fun mapRow(rs: java.sql.ResultSet): CategoryDto {
        return CategoryDto(
            id = rs.getString("id"),
            name = rs.getString("name"),
            isIncome = rs.getInt("isIncome") == 1,
            icon = rs.getString("icon") ?: "",
            parentId = rs.getString("parentId")?.takeIf { it.isNotEmpty() },
            isDefault = rs.getInt("isDefault") == 1,
            createdAt = rs.getLong("createdAt"),
            updatedAt = rs.getLong("updatedAt"),
            deletedAt = rs.getString("deletedAt")?.toLongOrNull(),
            syncedAt = rs.getLong("syncedAt").takeIf { it > 0 },
            serverReceivedAt = rs.getLong("serverReceivedAt").takeIf { it > 0 }
        )
    }
}