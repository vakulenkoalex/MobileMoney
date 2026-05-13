package com.mobilemoney.server

import java.util.UUID

object AuthService {

    fun login(login: String, password: String, deviceId: String, deviceName: String): Result<String> {
        val user = findUser(login)
        if (user == null) {
            return Result.failure(Exception("User not found"))
        }

        val salt = user["salt"] ?: ""
        val hash = sha256(password + salt)

        if (hash != user["passwordHash"]) {
            return Result.failure(Exception("Invalid password"))
        }

        val token = UUID.randomUUID().toString()
        insertDevice(deviceId, deviceName, token, login)

        return Result.success(token)
    }

    fun verify(token: String): Result<Device> {
        val device = findDeviceByToken(token)
        if (device == null) {
            return Result.failure(Exception("Invalid token"))
        }
        if (device["revokedAt"]?.isNotEmpty() == true) {
            return Result.failure(Exception("Token revoked"))
        }
        updateLastSeen(token)
        return Result.success(Device(
            device["deviceId"] ?: "",
            device["deviceName"] ?: "",
            device["login"] ?: ""
        ))
    }

    fun revoke(token: String): Result<Unit> {
        Database.getConnection().use { conn ->
            conn.prepareStatement("UPDATE devices SET revokedAt = ? WHERE token = ?").use { stmt ->
                stmt.setLong(1, System.currentTimeMillis())
                stmt.setString(2, token)
                stmt.executeUpdate()
            }
        }
        return Result.success(Unit)
    }
}

data class Device(val deviceId: String, val deviceName: String, val login: String)

fun findUser(login: String): Map<String, String>? {
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT login, passwordHash, salt FROM users WHERE login = ?").use { stmt ->
            stmt.setString(1, login)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return mapOf(
                        "login" to rs.getString("login"),
                        "passwordHash" to rs.getString("passwordHash"),
                        "salt" to rs.getString("salt")
                    )
                }
            }
        }
    }
    return null
}

fun findDeviceByToken(token: String): Map<String, String>? {
    Database.getConnection().use { conn ->
        conn.prepareStatement("SELECT deviceId, deviceName, token, login, revokedAt FROM devices WHERE token = ?").use { stmt ->
            stmt.setString(1, token)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val deviceId = rs.getString("deviceId")
                    val deviceName = rs.getString("deviceName")
                    val login = rs.getString("login")
                    val revokedAt = rs.getString("revokedAt")
                    println("Device found: deviceId=$deviceId, deviceName=$deviceName, login=$login, revokedAt=$revokedAt")
                    return mapOf(
                        "deviceId" to deviceId,
                        "deviceName" to deviceName,
                        "token" to rs.getString("token"),
                        "login" to login,
                        "revokedAt" to (revokedAt ?: "")
                    )
                }
            }
        }
    }
    return null
}

fun insertDevice(deviceId: String, deviceName: String, token: String, login: String) {
    val now = System.currentTimeMillis()
    Database.getConnection().use { conn ->
        conn.prepareStatement(
            "INSERT OR REPLACE INTO devices (deviceId, deviceName, token, login, createdAt, lastSeenAt) VALUES (?, ?, ?, ?, ?, ?)"
        ).use { stmt ->
            stmt.setString(1, deviceId)
            stmt.setString(2, deviceName)
            stmt.setString(3, token)
            stmt.setString(4, login)
            stmt.setLong(5, now)
            stmt.setLong(6, now)
            stmt.executeUpdate()
        }
    }
}

fun updateLastSeen(token: String) {
    Database.getConnection().use { conn ->
        conn.prepareStatement("UPDATE devices SET lastSeenAt = ? WHERE token = ?").use { stmt ->
            stmt.setLong(1, System.currentTimeMillis())
            stmt.setString(2, token)
            stmt.executeUpdate()
        }
    }
}