package com.mobilemoney.server

import java.sql.Connection

object Database {
    private var conn: java.sql.Connection? = null

    fun init(): Boolean {
        val dbPath = System.getenv("DB_PATH") ?: "data/sync.db"
        println("Database path: $dbPath")
        
        val jdbcUrl = "jdbc:sqlite:$dbPath"
        println("Connecting to: $jdbcUrl")
        
        conn = java.sql.DriverManager.getConnection(jdbcUrl)
        conn?.createStatement()?.use { stmt ->
            stmt.execute("PRAGMA journal_mode=WAL")
        }
        return createTables()
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
                        login VARCHAR(255) PRIMARY KEY,
                        password_hash VARCHAR(64) NOT NULL,
                        salt VARCHAR(32) NOT NULL
                    )
                """.trimIndent())
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS devices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        device_id VARCHAR(255) NOT NULL UNIQUE,
                        device_name VARCHAR(255),
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
                        deleted_at BIGINT
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
                        deleted_at BIGINT
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
                        deleted_at BIGINT
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

    fun getConnection(): Connection = conn
        ?: throw IllegalStateException("Database not initialized")

    fun isConnected(): Boolean = conn != null
}