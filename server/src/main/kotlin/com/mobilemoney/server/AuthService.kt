package com.mobilemoney.server

import java.util.UUID

object AuthService {

    fun login(login: String, password: String, deviceId: String, deviceName: String): Result<String> {
        val user = findUser(login)
        if (user == null) {
            println("Login failed: user not found: $login")
            return Result.failure(Exception("User not found"))
        }

        val salt = user["salt"] ?: ""
        val hash = sha256(password + salt)
        println("Login attempt: login=$login, inputHash=$hash, storedHash=${user["password_hash"]}, salt=$salt")
        
        if (hash != user["password_hash"]) {
            println("Login failed: invalid password for $login")
            return Result.failure(Exception("Invalid password"))
        }

        val token = UUID.randomUUID().toString()
        insertDevice(deviceId, deviceName, token, login)

        println("Login success: $login, token: $token")
        return Result.success(token)
    }

    fun verify(token: String): Result<Device> {
        val device = findDeviceByToken(token)
        if (device == null) {
            return Result.failure(Exception("Invalid token"))
        }
        if (device["revoked_at"] != null) {
            return Result.failure(Exception("Token revoked"))
        }
        updateLastSeen(token)
        return Result.success(Device(
            device["device_id"] ?: "",
            device["device_name"] ?: "",
            device["login"] ?: ""
        ))
    }

    fun revoke(token: String): Result<Unit> {
        Database.getConnection().use { conn ->
            conn.prepareStatement("UPDATE devices SET revoked_at = ? WHERE token = ?").use { stmt ->
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
        conn.prepareStatement("SELECT login, password_hash, salt FROM users WHERE login = ?").use { stmt ->
            stmt.setString(1, login)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return mapOf(
                        "login" to rs.getString("login"),
                        "password_hash" to rs.getString("password_hash"),
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
        conn.prepareStatement("SELECT device_id, device_name, token, login, revoked_at FROM devices WHERE token = ?").use { stmt ->
            stmt.setString(1, token)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return mapOf(
                        "device_id" to rs.getString("device_id"),
                        "device_name" to rs.getString("device_name"),
                        "token" to rs.getString("token"),
                        "login" to rs.getString("login"),
                        "revoked_at" to (rs.getString("revoked_at") ?: "")
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
            "INSERT INTO devices (device_id, device_name, token, login, created_at, last_seen_at) VALUES (?, ?, ?, ?, ?, ?)"
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
        conn.prepareStatement("UPDATE devices SET last_seen_at = ? WHERE token = ?").use { stmt ->
            stmt.setLong(1, System.currentTimeMillis())
            stmt.setString(2, token)
            stmt.executeUpdate()
        }
    }
}