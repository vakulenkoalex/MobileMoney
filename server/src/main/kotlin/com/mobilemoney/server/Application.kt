package com.mobilemoney.server

import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection

object SyncDatabase {
    private var db: HikariDataSource? = null
    private val devices = mutableMapOf<String, Pair<String, String>>()

    fun init(url: String, user: String, pass: String) {
        db = HikariDataSource().apply {
            setJdbcUrl(url)
            setUsername(user)
            setPassword(pass)
            maximumPoolSize = 10
        }
        createTables()
    }

    private fun createTables() {
        db?.connection?.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS devices (
                        id SERIAL PRIMARY KEY,
                        device_id VARCHAR(255) UNIQUE NOT NULL,
                        device_name VARCHAR(255),
                        token VARCHAR(255) UNIQUE NOT NULL,
                        created_at BIGINT NOT NULL,
                        last_seen_at BIGINT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }

    fun registerDevice(deviceId: String, deviceName: String, token: String) {
        val now = System.currentTimeMillis()
        try {
            db?.connection?.use { conn ->
                conn.prepareStatement("INSERT INTO devices (device_id, device_name, token, created_at, last_seen_at) VALUES (?, ?, ?, ?, ?)").use { stmt ->
                    stmt.setString(1, deviceId)
                    stmt.setString(2, deviceName)
                    stmt.setString(3, token)
                    stmt.setLong(4, now)
                    stmt.setLong(5, now)
                    stmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            println("DB error: ${e.message}")
        }
        devices[token] = deviceId to deviceName
    }

    fun validateToken(token: String): Boolean {
        if (devices.containsKey(token)) return true
        return false
    }

    fun isConnected(): Boolean = db != null
}

fun main() {
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/mobilemoney"
    val dbUser = System.getenv("DB_USER") ?: "postgres"
    val dbPass = System.getenv("DB_PASS") ?: "postgres"

    try {
        SyncDatabase.init(dbUrl, dbUser, dbPass)
        println("Database connected: $dbUrl")
    } catch (e: Exception) {
        println("Database not connected: ${e.message}")
    }

    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondText("{\"status\":\"ok\",\"database\":${SyncDatabase.isConnected()}}")
            }

            get("/api/v1/sync/register") {
                val deviceId = call.request.queryParameters["deviceId"] ?: "unknown"
                val deviceName = call.request.queryParameters["deviceName"] ?: "Unknown"
                val token = java.util.UUID.randomUUID().toString()
                SyncDatabase.registerDevice(deviceId, deviceName, token)
                println("Device registered: $deviceId")
                call.respondText("{\"token\":\"$token\",\"deviceId\":\"$deviceId\"}")
            }

            get("/api/v1/sync/changes") {
                val auth = call.request.headers["Authorization"]
                if (auth == null || !SyncDatabase.validateToken(auth)) {
                    call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                    return@get
                }
                call.respondText("{\"timestamp\":${System.currentTimeMillis()},\"accounts\":[],\"categories\":[],\"transactions\":[]}")
            }

            post("/api/v1/sync/push") {
                val auth = call.request.headers["Authorization"]
                if (auth == null || !SyncDatabase.validateToken(auth)) {
                    call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                    return@post
                }
                call.respondText("{\"success\":true,\"timestamp\":${System.currentTimeMillis()},\"synced\":0}")
            }

            get("/api/v1/sync/pull") {
                val auth = call.request.headers["Authorization"]
                if (auth == null || !SyncDatabase.validateToken(auth)) {
                    call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                    return@get
                }
                call.respondText("{\"timestamp\":${System.currentTimeMillis()},\"accounts\":[],\"categories\":[],\"transactions\":[]}")
            }
        }
    }.start(wait = true)
}