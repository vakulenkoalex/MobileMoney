package com.mobilemoney.server.model.entity

data class Device(
    val deviceId: String,
    val deviceName: String,
    val login: String,
    val token: String? = null,
    val revokedAt: Long? = null
)