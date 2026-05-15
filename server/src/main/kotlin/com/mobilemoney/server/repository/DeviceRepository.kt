package com.mobilemoney.server.repository

import com.mobilemoney.server.model.entity.Device

class DeviceRepository {

    fun findByToken(token: String): Device? {
        Database.getConnection().use { conn ->
            conn.prepareStatement("SELECT deviceId, deviceName, token, login, revokedAt FROM devices WHERE token = ?").use { stmt ->
                stmt.setString(1, token)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return Device(
                            deviceId = rs.getString("deviceId"),
                            deviceName = rs.getString("deviceName"),
                            login = rs.getString("login"),
                            token = rs.getString("token"),
                            revokedAt = rs.getString("revokedAt")?.toLongOrNull()
                        )
                    }
                }
            }
        }
        return null
    }

    fun upsert(device: Device) {
        val now = System.currentTimeMillis()
        Database.getConnection().use { conn ->
            conn.prepareStatement(
                "INSERT OR REPLACE INTO devices (deviceId, deviceName, token, login, createdAt, lastSeenAt, revokedAt) VALUES (?, ?, ?, ?, ?, ?, ?)"
            ).use { stmt ->
                stmt.setString(1, device.deviceId)
                stmt.setString(2, device.deviceName)
                stmt.setString(3, device.token ?: "")
                stmt.setString(4, device.login)
                stmt.setLong(5, now)
                stmt.setLong(6, now)
                stmt.setString(7, device.revokedAt?.toString())
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

    fun revoke(token: String) {
        Database.getConnection().use { conn ->
            conn.prepareStatement("UPDATE devices SET revokedAt = ? WHERE token = ?").use { stmt ->
                stmt.setLong(1, System.currentTimeMillis())
                stmt.setString(2, token)
                stmt.executeUpdate()
            }
        }
    }
}