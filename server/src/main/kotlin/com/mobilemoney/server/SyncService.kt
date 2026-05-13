package com.mobilemoney.server

object SyncService {

    fun push(data: SyncPushRequestDto): SyncResponse {
        var syncedCount = 0

        data.currencies.forEach { currency ->
            upsertCurrency(currency)
            syncedCount++
        }

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
            currencies = getCurrencies(since),
            accounts = getAccounts(since),
            categories = getCategories(since),
            transactions = getTransactions(since)
        )
    }

    fun pull(): SyncChangesResponse {
        return SyncChangesResponse(
            timestamp = System.currentTimeMillis(),
            currencies = getAllCurrencies(),
            accounts = getAllAccounts(),
            categories = getAllCategories(),
            transactions = getAllTransactions()
        )
    }
}

data class SyncResponse(
    val success: Boolean,
    val timestamp: Long,
    val synced: Int
)

data class SyncChangesResponse(
    val timestamp: Long,
    val currencies: List<String> = emptyList(),
    val accounts: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val transactions: List<String> = emptyList()
)

fun upsertAccount(data: AccountDto) {
    val incomingUpdatedAt = data.updatedAt
    val existingUpdatedAt = getAccountUpdatedAt(data.id)

    if (existingUpdatedAt != null && incomingUpdatedAt <= existingUpdatedAt) {
        return
    }

    val serverReceivedAt = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO accounts (id, name, typeId, currencyCode, icon, isDefault, archived, createdAt, updatedAt, deletedAt, serverReceivedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, data.id)
            stmt.setString(2, data.name)
            stmt.setString(3, data.typeId)
            stmt.setString(4, data.currencyCode)
            stmt.setString(5, data.icon)
            stmt.setInt(6, if (data.isDefault) 1 else 0)
            stmt.setInt(7, if (data.archived) 1 else 0)
            stmt.setLong(8, data.createdAt)
            stmt.setLong(9, data.updatedAt)
            stmt.setString(10, data.deletedAt?.toString())
            stmt.setLong(11, serverReceivedAt)
            stmt.executeUpdate()
        }
    }
}

fun getAccountUpdatedAt(id: String): Long? {
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT updatedAt FROM accounts WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getLong("updatedAt")
                }
            }
        }
    }
    return null
}

fun upsertCategory(data: CategoryDto) {
    val incomingUpdatedAt = data.updatedAt
    val existingUpdatedAt = getCategoryUpdatedAt(data.id)

    if (existingUpdatedAt != null && incomingUpdatedAt <= existingUpdatedAt) {
        return
    }

    val serverReceivedAt = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO categories (id, name, is_income, icon, parent_id, created_at, updated_at, deleted_at, server_received_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, data.id)
            stmt.setString(2, data.name)
            stmt.setInt(3, if (data.isIncome) 1 else 0)
            stmt.setString(4, data.icon)
            stmt.setString(5, data.parentId)
            stmt.setLong(6, data.createdAt)
            stmt.setLong(7, data.updatedAt)
            stmt.setString(8, data.deletedAt?.toString())
            stmt.setLong(9, serverReceivedAt)
            stmt.executeUpdate()
        }
    }
}

fun getCategoryUpdatedAt(id: String): Long? {
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT updated_at FROM categories WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getLong("updated_at")
                }
            }
        }
    }
    return null
}

fun upsertTransaction(data: TransactionDto) {
    val incomingUpdatedAt = data.updatedAt
    val existingUpdatedAt = getTransactionUpdatedAt(data.id)

    if (existingUpdatedAt != null && incomingUpdatedAt <= existingUpdatedAt) {
        return
    }

    val serverReceivedAt = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO transactions (id, account_id, category_id, amount, date, comment, source, source_data, creator_id, related_transaction_id, created_at, updated_at, deleted_at, server_received_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.setLong(11, data.createdAt)
            stmt.setLong(12, data.updatedAt)
            stmt.setString(13, data.deletedAt?.toString())
            stmt.setLong(14, serverReceivedAt)
            stmt.executeUpdate()
        }
    }
}

fun getTransactionUpdatedAt(id: String): Long? {
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT updated_at FROM transactions WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getLong("updated_at")
                }
            }
        }
    }
    return null
}

fun getAccounts(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM accounts WHERE serverReceivedAt > ?").use { stmt ->
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
        conn.prepareStatement("SELECT * FROM accounts WHERE deletedAt IS NULL").use { stmt ->
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
    val serverReceivedAt = rs.getLong("serverReceivedAt")
    val deletedAtVal = rs.getString("deletedAt")
    val isDefault = rs.getInt("isDefault") == 1
    return """{"id":"${rs.getString("id")}","name":"${rs.getString("name")}","typeId":"${rs.getString("typeId")}","currencyCode":"${rs.getString("currencyCode") ?: ""}","icon":"${rs.getString("icon") ?: ""}","isDefault":$isDefault,"createdAt":${rs.getLong("createdAt")},"updatedAt":${rs.getLong("updatedAt")},"deletedAt":${if (deletedAtVal != null) deletedAtVal else "null"},"serverReceivedAt":${if (serverReceivedAt > 0) serverReceivedAt else "null"}}"""
}

fun getCategories(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM categories WHERE server_received_at > ?").use { stmt ->
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
    val serverReceivedAt = rs.getLong("server_received_at")
    return """{"id":"${rs.getString("id")}","name":"${rs.getString("name")}","is_income":${rs.getInt("is_income")},"icon":"${rs.getString("icon") ?: ""}","parent_id":${rs.getString("parent_id")?.let { "\"$it\"" } ?: "null"},"created_at":${rs.getLong("created_at")},"updated_at":${rs.getLong("updated_at")},"deleted_at":${rs.getString("deleted_at") ?: "null"},"server_received_at":${if (serverReceivedAt > 0) serverReceivedAt else "null"}}"""
}

fun getTransactions(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM transactions WHERE server_received_at > ?").use { stmt ->
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
    val serverReceivedAt = rs.getLong("server_received_at")
    return """{"id":"${rs.getString("id")}","account_id":"${rs.getString("account_id")}","category_id":${rs.getString("category_id")?.let { "\"$it\"" } ?: "null"},"amount":${rs.getDouble("amount")},"date":${rs.getLong("date")},"comment":"${rs.getString("comment") ?: ""}","creator_id":${rs.getString("creator_id")?.let { "\"$it\"" } ?: "null"},"created_at":${rs.getLong("created_at")},"updated_at":${rs.getLong("updated_at")},"deleted_at":${rs.getString("deleted_at") ?: "null"},"server_received_at":${if (serverReceivedAt > 0) serverReceivedAt else "null"}}"""
}

fun upsertCurrency(data: CurrencyDto) {
    val serverReceivedAt = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO currencies (code, name, symbol, created_at, updated_at, server_received_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, data.code)
            stmt.setString(2, data.name)
            stmt.setString(3, data.symbol)
            stmt.setLong(4, data.createdAt)
            stmt.setLong(5, data.updatedAt)
            stmt.setLong(6, serverReceivedAt)
            stmt.executeUpdate()
        }
    }
}

fun getCurrencies(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM currencies WHERE server_received_at > ?").use { stmt ->
            stmt.setLong(1, since)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonCurrency(rs))
                }
            }
        }
    }
    return result
}

fun getAllCurrencies(): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM currencies").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(buildJsonCurrency(rs))
                }
            }
        }
    }
    return result
}

fun buildJsonCurrency(rs: java.sql.ResultSet): String {
    val serverReceivedAt = rs.getLong("server_received_at")
    return """{"code":"${rs.getString("code")}","name":"${rs.getString("name")}","symbol":"${rs.getString("symbol")}","created_at":${rs.getLong("created_at")},"updated_at":${rs.getLong("updated_at")},"server_received_at":${if (serverReceivedAt > 0) serverReceivedAt else "null"}}"""
}