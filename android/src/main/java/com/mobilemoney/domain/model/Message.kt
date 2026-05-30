package com.mobilemoney.domain.model

data class Message(
    val id: String,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val processed: Boolean = false,
    val error: String? = null,
    val transactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
