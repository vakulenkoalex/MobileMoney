package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncPushResponse(
    val success: Boolean,
    val timestamp: Long,
    val synced: Int
)