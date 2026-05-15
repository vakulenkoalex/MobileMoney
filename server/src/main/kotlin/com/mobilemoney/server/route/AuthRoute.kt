package com.mobilemoney.server.route

import com.mobilemoney.server.DI
import com.mobilemoney.server.model.dto.LoginRequest
import com.mobilemoney.server.service.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Routing.authRoutes() {
    val authService: AuthService = DI.authService

    post("/api/v1/auth/login") {
        val body = call.receiveText()
        val loginRequest = try {
            Json.decodeFromString<LoginRequest>(body)
        } catch (e: Exception) {
            call.respondText("{\"error\":\"Invalid request body\"}", status = HttpStatusCode.BadRequest)
            return@post
        }

        if (loginRequest.login.isEmpty() || loginRequest.password.isEmpty()) {
            call.respondText("{\"error\":\"Missing login or password\"}", status = HttpStatusCode.BadRequest)
            return@post
        }

        val result = authService.login(
            loginRequest.login,
            loginRequest.password,
            loginRequest.deviceId,
            loginRequest.deviceName
        )

        if (result.isFailure) {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            val status = if (error.contains("not found")) HttpStatusCode.NotFound else HttpStatusCode.Unauthorized
            call.respondText("{\"error\":\"$error\"}", status = status)
            return@post
        }

        call.respondText("{\"token\":\"${result.getOrNull()}\",\"login\":\"${loginRequest.login}\"}")
    }
}