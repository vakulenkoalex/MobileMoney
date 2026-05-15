package com.mobilemoney.server

object SyncService {

    fun push(data: SyncPushRequestDto): SyncResponse {
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

fun upsertAccount(data: AccountDto) {
    val serverReceivedAt = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO accounts (id, name, typeId, currencyCode, icon, isDefault, archived, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.setString(11, data.syncedAt?.toString())
            stmt.setLong(12, serverReceivedAt)
            stmt.executeUpdate()
        }
    }
}

fun upsertCategory(data: CategoryDto) {
    val serverReceivedAt = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO categories (id, name, isIncome, icon, parentId, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            stmt.setString(1, data.id)
            stmt.setString(2, data.name)
            stmt.setInt(3, if (data.isIncome) 1 else 0)
            stmt.setString(4, data.icon)
            stmt.setString(5, data.parentId)
            stmt.setLong(6, data.createdAt)
            stmt.setLong(7, data.updatedAt)
            stmt.setString(8, data.deletedAt?.toString())
            stmt.setString(9, data.syncedAt?.toString())
            stmt.setLong(10, serverReceivedAt)
            stmt.executeUpdate()
        }
    }
}

fun upsertTransaction(data: TransactionDto) {
    val serverReceivedAt = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement("""
            INSERT OR REPLACE INTO transactions (id, accountId, categoryId, amount, date, comment, source, sourceData, creatorId, relatedTransactionId, createdAt, updatedAt, deletedAt, syncedAt, serverReceivedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.setString(14, data.syncedAt?.toString())
            stmt.setLong(15, serverReceivedAt)
            stmt.executeUpdate()
        }
    }
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
    val syncedAt = rs.getLong("syncedAt")
    val serverReceivedAt = rs.getLong("serverReceivedAt")
    val deletedAtVal = rs.getString("deletedAt")
    val isDefault = rs.getInt("isDefault") == 1
    return """{"id":"${rs.getString("id")}","name":"${rs.getString("name")}","typeId":"${rs.getString("typeId")}","currencyCode":"${rs.getString("currencyCode") ?: ""}","icon":"${rs.getString("icon") ?: ""}","isDefault":$isDefault,"createdAt":${rs.getLong("createdAt")},"updatedAt":${rs.getLong("updatedAt")},"deletedAt":${if (deletedAtVal != null) deletedAtVal else "null"},"syncedAt":${if (syncedAt > 0) syncedAt else "null"},"serverReceivedAt":${if (serverReceivedAt > 0) serverReceivedAt else "null"}}"""
}

fun getCategories(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM categories WHERE serverReceivedAt > ?").use { stmt ->
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
        conn.prepareStatement("SELECT * FROM categories WHERE deletedAt IS NULL").use { stmt ->
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
    val syncedAt = rs.getLong("syncedAt")
    val serverReceivedAt = rs.getLong("serverReceivedAt")
    val deletedAtVal = rs.getString("deletedAt")
    val isIncome = rs.getInt("isIncome") == 1
    val parentId = rs.getString("parentId")
    return """{"id":"${rs.getString("id")}","name":"${rs.getString("name")}","isIncome":$isIncome,"icon":"${rs.getString("icon") ?: ""}","parentId":${if (parentId != null) "\"$parentId\"" else "null"},"createdAt":${rs.getLong("createdAt")},"updatedAt":${rs.getLong("updatedAt")},"deletedAt":${if (deletedAtVal != null) deletedAtVal else "null"},"syncedAt":${if (syncedAt > 0) syncedAt else "null"},"serverReceivedAt":${if (serverReceivedAt > 0) serverReceivedAt else "null"}}"""
}

fun getTransactions(since: Long): List<String> {
    val result = mutableListOf<String>()
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT * FROM transactions WHERE serverReceivedAt > ?").use { stmt ->
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
        conn.prepareStatement("SELECT * FROM transactions WHERE deletedAt IS NULL").use { stmt ->
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
    val syncedAt = rs.getLong("syncedAt")
    val serverReceivedAt = rs.getLong("serverReceivedAt")
    val deletedAtVal = rs.getString("deletedAt")
    val categoryId = rs.getString("categoryId")
    val creatorId = rs.getString("creatorId")
    return """{"id":"${rs.getString("id")}","accountId":"${rs.getString("accountId")}","categoryId":${if (categoryId != null) "\"$categoryId\"" else "null"},"amount":${rs.getDouble("amount")},"date":${rs.getLong("date")},"comment":"${rs.getString("comment") ?: ""}","creatorId":${if (creatorId != null) "\"$creatorId\"" else "null"},"createdAt":${rs.getLong("createdAt")},"updatedAt":${rs.getLong("updatedAt")},"deletedAt":${if (deletedAtVal != null) deletedAtVal else "null"},"syncedAt":${if (syncedAt > 0) syncedAt else "null"},"serverReceivedAt":${if (serverReceivedAt > 0) serverReceivedAt else "null"}}"""
}