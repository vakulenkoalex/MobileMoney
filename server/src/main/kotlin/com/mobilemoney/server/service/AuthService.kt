package com.mobilemoney.server.service

import com.mobilemoney.server.model.entity.Device
import com.mobilemoney.server.sha512
import com.mobilemoney.server.repository.DeviceRepository
import com.mobilemoney.server.repository.UserRepository
import java.util.UUID

class AuthService(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository
) {

    fun login(login: String, password: String, deviceId: String, deviceName: String): Result<String> {
        val user = userRepository.findByLogin(login)
            ?: return Result.failure(Exception("User not found"))

        val hash = sha512(password + user.salt)
        if (hash != user.passwordHash) {
            return Result.failure(Exception("Invalid password"))
        }

        val token = UUID.randomUUID().toString()
        deviceRepository.upsert(Device(
            deviceId = deviceId,
            deviceName = deviceName,
            login = login,
            token = token
        ))

        return Result.success(token)
    }

    fun verify(token: String): Result<Device> {
        val device = deviceRepository.findByToken(token)
            ?: return Result.failure(Exception("Invalid token"))

        if (device.revokedAt != null) {
            return Result.failure(Exception("Token revoked"))
        }

        deviceRepository.updateLastSeen(token)
        return Result.success(device)
    }

}

