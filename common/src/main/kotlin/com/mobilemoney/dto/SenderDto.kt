package com.mobilemoney.dto

import kotlinx.serialization.Serializable

@Serializable
data class SenderDto(
    val id: String,
    val sender: String,
    val label: String = "",
    val type: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)
