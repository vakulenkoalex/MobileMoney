package com.mobilemoney.server.repository

import java.sql.Connection

object Database {
    private var conn: Connection? = null
    private var initialized = false
    private var jdbcUrl: String = ""

    fun init(): Boolean {
        if (initialized) return true

        val dbPath = System.getenv("DB_PATH") ?: "data/sync.db"
        println("Database path: $dbPath")

        jdbcUrl = "jdbc:sqlite:$dbPath"
        conn = java.sql.DriverManager.getConnection(jdbcUrl)
        conn?.createStatement()?.use { stmt ->
            stmt.execute("PRAGMA journal_mode=WAL")
        }
        initialized = true
        createTables()
        insertDefaultData()
        return true
    }

    private fun createTables() {
        val existingTables = mutableListOf<String>()
        conn?.createStatement()?.use { stmt ->
            val rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")
            while (rs.next()) {
                existingTables.add(rs.getString(1))
            }
        }

        if (existingTables.isNotEmpty()) {
            return
        }

        println("Creating tables...")
        conn?.use { c ->
            c.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        login TEXT PRIMARY KEY NOT NULL,
                        passwordHash TEXT NOT NULL,
                        salt TEXT NOT NULL
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS devices (
                        deviceId TEXT NOT NULL UNIQUE,
                        deviceName TEXT NOT NULL,
                        token TEXT UNIQUE NOT NULL,
                        login TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastSeenAt INTEGER NOT NULL,
                        revokedAt INTEGER
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        typeId TEXT NOT NULL,
                        currencyCode TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        archived INTEGER NOT NULL DEFAULT 0,
                        autoCreateEnabled INTEGER NOT NULL DEFAULT 0,
                        cardMask TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        syncedAt INTEGER,
                        serverReceivedAt INTEGER
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        isIncome INTEGER NOT NULL,
                        icon TEXT NOT NULL,
                        parentId TEXT,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        syncedAt INTEGER,
                        serverReceivedAt INTEGER
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS message_regexes (
                        id TEXT PRIMARY KEY NOT NULL,
                        label TEXT NOT NULL,
                        pattern TEXT NOT NULL,
                        skipBalanceCheck INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        syncedAt INTEGER,
                        serverReceivedAt INTEGER
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS senders (
                        id TEXT PRIMARY KEY NOT NULL,
                        sender TEXT NOT NULL,
                        label TEXT NOT NULL DEFAULT '',
                        type TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        syncedAt INTEGER,
                        serverReceivedAt INTEGER
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id TEXT PRIMARY KEY NOT NULL,
                        accountId TEXT NOT NULL,
                        categoryId TEXT NOT NULL,
                        amount REAL NOT NULL,
                        date INTEGER NOT NULL,
                        comment TEXT,
                        source TEXT NOT NULL,
                        sourceData TEXT,
                        creatorId TEXT,
                        relatedTransactionId TEXT,
                        shop TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        syncedAt INTEGER,
                        serverReceivedAt INTEGER
                    )
                """.trimIndent())

                val rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")
                println("Tables created:")
                while (rs.next()) {
                    println("  - ${rs.getString(1)}")
                }
            }
        }
    }

    fun getConnection(): Connection {
        if (conn == null || conn!!.isClosed) {
            println("Reconnecting to database...")
            conn = java.sql.DriverManager.getConnection(jdbcUrl)
        }
        return conn!!
    }

    fun isConnected(): Boolean = conn != null

    private fun insertDefaultData() {
        val existingCategories = conn?.createStatement()?.use { stmt ->
            val rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")
            if (rs.next()) rs.getInt(1) > 0 else false
        } ?: false

        if (existingCategories) return

        val now = System.currentTimeMillis()
        val conn = getConnection()

        conn.prepareStatement("""
            INSERT INTO categories (id, name, isIncome, icon, parentId, createdAt, updatedAt, deletedAt, serverReceivedAt)
            VALUES (?, ?, ?, ?, NULL, ?, ?, NULL, ?)
        """).use { stmt ->
            listOf(
                Triple(java.util.UUID.randomUUID().toString(), "Кафе и рестораны", "restaurant"),
                Triple(java.util.UUID.randomUUID().toString(), "Развлечения", "movie"),
                Triple(java.util.UUID.randomUUID().toString(), "Здоровье", "local_hospital"),
                Triple(java.util.UUID.randomUUID().toString(), "Зарплата", "work"),
                Triple(java.util.UUID.randomUUID().toString(), "Подарок", "card_giftcard")
            ).forEachIndexed { index, (id, name, icon) ->
                stmt.setString(1, id)
                stmt.setString(2, name)
                stmt.setInt(3, if (index in 3..4) 1 else 0)
                stmt.setString(4, icon)
                stmt.setLong(5, now)
                stmt.setLong(6, now)
                stmt.setLong(7, now)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }

        val adjustExpenseId = java.util.UUID.randomUUID().toString()
        val adjustIncomeId = java.util.UUID.randomUUID().toString()
        conn.prepareStatement("""
            INSERT INTO categories (id, name, isIncome, icon, parentId, isDefault, createdAt, updatedAt, deletedAt, serverReceivedAt)
            VALUES (?, 'Корректировка', ?, 'more_horiz', NULL, 1, ?, ?, NULL, ?)
        """).use { stmt ->
            stmt.setString(1, adjustExpenseId)
            stmt.setInt(2, 0)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.executeUpdate()
            stmt.setString(1, adjustIncomeId)
            stmt.setInt(2, 1)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.executeUpdate()
        }

        val accountId = java.util.UUID.randomUUID().toString()
        conn.prepareStatement("""
            INSERT INTO accounts (id, name, typeId, currencyCode, icon, isDefault, archived, autoCreateEnabled, cardMask, createdAt, updatedAt, deletedAt, serverReceivedAt)
            VALUES (?, 'Наличные', 'cash', 'RUB', 'wallet', 1, 0, 0, NULL, ?, ?, NULL, ?)
        """).use { stmt ->
            stmt.setString(1, accountId)
            stmt.setLong(2, now)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.executeUpdate()
        }
        val transferExpenseId = com.mobilemoney.dto.TransferConstants.EXPENSE_CATEGORY_ID
        val transferIncomeId = com.mobilemoney.dto.TransferConstants.INCOME_CATEGORY_ID
        conn.prepareStatement("""
            INSERT INTO categories (id, name, isIncome, icon, parentId, isDefault, createdAt, updatedAt, deletedAt, serverReceivedAt)
            VALUES (?, 'Перемещение', ?, 'swap_horiz', NULL, 0, ?, ?, NULL, ?)
        """).use { stmt ->
            stmt.setString(1, transferExpenseId)
            stmt.setInt(2, 0)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.executeUpdate()
            stmt.setString(1, transferIncomeId)
            stmt.setInt(2, 1)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.executeUpdate()
        }

        println("Default data inserted")
    }
}