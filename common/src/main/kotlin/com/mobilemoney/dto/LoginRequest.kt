package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
    val deviceId: String,
    val deviceName: String
)