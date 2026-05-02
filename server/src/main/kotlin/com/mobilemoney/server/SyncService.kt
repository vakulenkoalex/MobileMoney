package com.mobilemoney.server

object SyncService {

    fun push(data: SyncRequest): SyncResponse {
        var syncedCount = 0

        data.accounts.forEach { account ->
            upsertAccount(account)
            syncedCount++
        }

        data.categories.forEach { category ->
            upsertCategory(category)
            syncedCount++
        }

        data.transactions.forEach { transaction ->
            upsertTransaction(transaction)
            syncedCount++
        }

        println("Push: $syncedCount items")
        return SyncResponse(success = true, timestamp = System.currentTimeMillis(), synced = syncedCount)
    }

    fun getChanges(since: Long): SyncChangesResponse {
        return SyncChangesResponse(
            timestamp = System.currentTimeMillis(),
            accounts = getAccounts(since),
            categories = getCategories(since),
            transactions = getTransactions(since)
        )
    }

    fun pull(): SyncChangesResponse {
        return SyncChangesResponse(
            timestamp = System.currentTimeMillis(),
            accounts = getAllAccounts(),
            categories = getAllCategories(),
            transactions = getAllTransactions()
        )
    }
}

data class SyncRequest(
    val accounts: List<Map<String, String>> = emptyList(),
    val categories: List<Map<String, String>> = emptyList(),
    val transactions: List<Map<String, String>> = emptyList()
)

data class SyncResponse(
    val success: Boolean,
    val timestamp: Long,
    val synced: Int
)

data class SyncChangesResponse(
    val timestamp: Long,
    val accounts: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val transactions: List<String> = emptyList()
)

fun upsertAccount(data: Map<String, String>) {
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO accounts (id, name, type_id, currency_code, icon, is_default, archived, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, data["id"] ?: return)
            stmt.setString(2, data["name"] ?: "")
            stmt.setString(3, data["type_id"] ?: "cash")
            stmt.setString(4, data["currency_code"])
            stmt.setString(5, data["icon"])
            stmt.setInt(6, (data["is_default"] ?: "false").toInt())
            stmt.setInt(7, (data["archived"] ?: "false").toInt())
            stmt.setLong(8, (data["created_at"] ?: "0").toLong())
            stmt.setLong(9, (data["updated_at"] ?: "0").toLong())
            stmt.setString(10, data["deleted_at"])
            stmt.executeUpdate()
        }
    }
}

fun upsertCategory(data: Map<String, String>) {
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO categories (id, name, is_income, icon, parent_id, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, data["id"] ?: return)
            stmt.setString(2, data["name"] ?: "")
            stmt.setInt(3, (data["is_income"] ?: "false").toInt())
            stmt.setString(4, data["icon"])
            stmt.setString(5, data["parent_id"])
            stmt.setLong(6, (data["created_at"] ?: "0").toLong())
            stmt.setLong(7, (data["updated_at"] ?: "0").toLong())
            stmt.setString(8, data["deleted_at"])
            stmt.executeUpdate()
        }
    }
}

fun upsertTransaction(data: Map<String, String>) {
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO transactions (id, account_id, category_id, amount, date, comment, source, source_data, creator_id, related_transaction_id, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, data["id"] ?: return)
            stmt.setString(2, data["account_id"] ?: return)
            stmt.setString(3, data["category_id"])
            stmt.setDouble(4, (data["amount"] ?: "0").toDouble())
            stmt.setLong(5, (data["date"] ?: "0").toLong())
            stmt.setString(6, data["comment"])
            stmt.setString(7, data["source"])
            stmt.setString(8, data["source_data"])
            stmt.setString(9, data["creator_id"])
            stmt.setString(10, data["related_transaction_id"])
            stmt.setLong(11, (data["created_at"] ?: "0").toLong())
            stmt.setLong(12, (data["updated_at"] ?: "0").toLong())
            stmt.setString(13, data["deleted_at"])
            stmt.executeUpdate()
        }
    }
}

fun getAccounts(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM accounts WHERE updated_at > ?").use { stmt ->
            stmt.setLong(1, since)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonAccount(rs))
                }
            }
        }
    }
    return result
}

fun getAllAccounts(): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM accounts WHERE deleted_at IS NULL").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonAccount(rs))
                }
            }
        }
    }
    return result
}

fun buildJsonAccount(rs: java.sql.ResultSet): String {
    return """{"id":"${rs.getString("id")}","name":"${rs.getString("name")}","type_id":"${rs.getString("type_id")}","currency_code":"${rs.getString("currency_code") ?: ""}","icon":"${rs.getString("icon") ?: ""}","is_default":${rs.getInt("is_default")},"created_at":${rs.getLong("created_at")},"updated_at":${rs.getLong("updated_at")},"deleted_at":${rs.getString("deleted_at") ?: "null"}}"""
}

fun getCategories(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM categories WHERE updated_at > ?").use { stmt ->
            stmt.setLong(1, since)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonCategory(rs))
                }
            }
        }
    }
    return result
}

fun getAllCategories(): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM categories WHERE deleted_at IS NULL").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonCategory(rs))
                }
            }
        }
    }
    return result
}

fun buildJsonCategory(rs: java.sql.ResultSet): String {
    return """{"id":"${rs.getString("id")}","name":"${rs.getString("name")}","is_income":${rs.getInt("is_income")},"icon":"${rs.getString("icon") ?: ""}","parent_id":${rs.getString("parent_id")?.let { "\"$it\"" } ?: "null"},"created_at":${rs.getLong("created_at")},"updated_at":${rs.getLong("updated_at")},"deleted_at":${rs.getString("deleted_at") ?: "null"}}"""
}

fun getTransactions(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM transactions WHERE updated_at > ?").use { stmt ->
            stmt.setLong(1, since)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonTransaction(rs))
                }
            }
        }
    }
    return result
}

fun getAllTransactions(): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM transactions WHERE deleted_at IS NULL").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonTransaction(rs))
                }
            }
        }
    }
    return result
}

fun buildJsonTransaction(rs: java.sql.ResultSet): String {
    return """{"id":"${rs.getString("id")}","account_id":"${rs.getString("account_id")}","category_id":${rs.getString("category_id")?.let { "\"$it\"" } ?: "null"},"amount":${rs.getDouble("amount")},"date":${rs.getLong("date")},"comment":"${rs.getString("comment") ?: ""}","creator_id":${rs.getString("creator_id")?.let { "\"$it\"" } ?: "null"},"created_at":${rs.getLong("created_at")},"updated_at":${rs.getLong("updated_at")},"deleted_at":${rs.getString("deleted_at") ?: "null"}}"""
}