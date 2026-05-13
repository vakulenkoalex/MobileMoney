package com.mobilemoney.server

import java.sql.Connection

object Database {
    private var conn: java.sql.Connection? = null
    private var initialized = false
    private var jdbcUrl: String = ""

    fun init(): Boolean {
        if (initialized) return true

        val dbPath = System.getenv("DB_PATH") ?: "data/sync.db"
        println("Database path: $dbPath")

        jdbcUrl = "jdbc:sqlite:$dbPath"
        println("Connecting to: $jdbcUrl")

        conn = java.sql.DriverManager.getConnection(jdbcUrl)
        conn?.createStatement()?.use { stmt ->
            stmt.execute("PRAGMA journal_mode=WAL")
        }
        initialized = true
        val tablesCreated = createTables()
        if (tablesCreated) {
            insertDefaultData()
        }
        return tablesCreated
    }

    private fun createTables(): Boolean {
        val existingTables = mutableListOf<String>()
        conn?.createStatement()?.use { stmt ->
            val rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")
            while (rs.next()) {
                existingTables.add(rs.getString(1))
            }
        }

        if (existingTables.isNotEmpty()) {
            return false
        }

        println("Creating tables...")
        conn?.use { c ->
            c.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        login VARCHAR(255) PRIMARY KEY NOT NULL,
                        password_hash VARCHAR(64) NOT NULL,
                        salt VARCHAR(32) NOT NULL
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS devices (
                        device_id VARCHAR(255) NOT NULL UNIQUE,
                        device_name VARCHAR(255) NOT NULL,
                        token VARCHAR(255) UNIQUE NOT NULL,
                        login VARCHAR(255) NOT NULL,
                        created_at BIGINT NOT NULL,
                        last_seen_at BIGINT NOT NULL,
                        revoked_at BIGINT
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id VARCHAR(255) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        type_id VARCHAR(50) NOT NULL,
                        currency_code VARCHAR(10),
                        icon VARCHAR(50),
                        is_default INTEGER DEFAULT 0,
                        archived INTEGER DEFAULT 0,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        deleted_at BIGINT,
                        server_received_at BIGINT
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id VARCHAR(255) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        is_income INTEGER NOT NULL,
                        icon VARCHAR(50),
                        parent_id VARCHAR(255),
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        deleted_at BIGINT,
                        server_received_at BIGINT
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS currencies (
                        code VARCHAR(10) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        symbol VARCHAR(10) NOT NULL,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        server_received_at BIGINT
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id VARCHAR(255) PRIMARY KEY,
                        account_id VARCHAR(255) NOT NULL,
                        category_id VARCHAR(255),
                        amount REAL NOT NULL,
                        date BIGINT NOT NULL,
                        comment TEXT,
                        source VARCHAR(50),
                        source_data TEXT,
                        creator_id VARCHAR(255),
                        related_transaction_id VARCHAR(255),
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        deleted_at BIGINT,
                        server_received_at BIGINT
                    )
                """.trimIndent())
                
                // Check tables
                val rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")
                println("Tables in database:")
                while (rs.next()) {
                    println("  - ${rs.getString(1)}")
                }
            }
        }
        return true
    }

    fun getConnection(): Connection {
        if (conn == null || conn!!.isClosed) {
            println("Reconnecting to database...")
            conn = java.sql.DriverManager.getConnection(jdbcUrl)
        }
        return conn!!
    }

    fun isConnected(): Boolean = conn != null

    fun insertDefaultData() {
        val now = System.currentTimeMillis()
        val conn = getConnection()

        conn.prepareStatement("SELECT COUNT(*) FROM currencies").use { stmt ->
            stmt.executeQuery().use { rs ->
                if (rs.next() && rs.getInt(1) > 0) {
                    println("Default data already exists, skipping")
                    return
                }
            }
        }

        conn.prepareStatement("""
            INSERT INTO currencies (code, name, symbol, created_at, updated_at, server_received_at) VALUES (?, ?, ?, ?, ?, ?)
        """).use { stmt ->
            listOf(
                Triple("RUB", "Российский рубль", "₽"),
                Triple("USD", "Доллар США", "$"),
                Triple("EUR", "Евро", "€")
            ).forEach { (code, name, symbol) ->
                stmt.setString(1, code)
                stmt.setString(2, name)
                stmt.setString(3, symbol)
                stmt.setLong(4, now)
                stmt.setLong(5, now)
                stmt.setLong(6, now)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
        println("Default currencies inserted")

        conn.prepareStatement("""
            INSERT INTO categories (id, name, is_income, icon, parent_id, created_at, updated_at, deleted_at, server_received_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?)
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
                stmt.setString(5, null)
                stmt.setLong(6, now)
                stmt.setLong(7, now)
                stmt.setLong(8, now)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
        val adjustExpenseId = java.util.UUID.randomUUID().toString()
        val adjustIncomeId = java.util.UUID.randomUUID().toString()
        conn.prepareStatement("""
            INSERT INTO categories (id, name, is_income, icon, parent_id, created_at, updated_at, deleted_at, server_received_at)
            VALUES (?, 'Корректировка', ?, 'more_horiz', NULL, ?, ?, NULL, ?)
        """).use { stmt ->
            stmt.setString(1, adjustExpenseId)
            stmt.setInt(2, 0)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.addBatch()
            stmt.setString(1, adjustIncomeId)
            stmt.setInt(2, 1)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.setLong(5, now)
            stmt.addBatch()
            stmt.executeBatch()
        }
        println("Default categories inserted")

        val accountId = java.util.UUID.randomUUID().toString()
        conn.prepareStatement("""
            INSERT INTO accounts (id, name, type_id, currency_code, icon, is_default, archived, created_at, updated_at, deleted_at, server_received_at)
            VALUES (?, 'Наличные', 'cash', 'RUB', 'wallet', 1, 0, ?, ?, NULL, ?)
        """).use { stmt ->
            stmt.setString(1, accountId)
            stmt.setLong(2, now)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.executeUpdate()
        }
        println("Default account inserted")
    }
}