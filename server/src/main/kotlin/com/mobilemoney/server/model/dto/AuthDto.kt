package com.mobilemoney.server.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
    val deviceId: String,
    val deviceName: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val login: String
)

@Serializable
data class AuthError(
    val error: String,
    val code: String? = null
)