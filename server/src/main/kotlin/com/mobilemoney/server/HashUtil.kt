package com.mobilemoney.server

import java.nio.charset.StandardCharsets

fun sha256(input: String): String {
    val bytes = input.toByteArray(StandardCharsets.UTF_8)
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(bytes)
    return hash.joinToString("") { "%02x".format(it) }
}