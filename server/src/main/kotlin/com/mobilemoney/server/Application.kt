package com.mobilemoney.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    val nettyPort = (System.getenv("NETTY_PORT")).toInt()

    val dbInitialized = Database.init()
    if (dbInitialized) {
        println("Database initialized: sync.db")
    }

    embeddedServer(Netty, port = nettyPort) {
        routing {
            get("/") {
                call.respondText("{\"status\":\"ok\",\"database\":${Database.isConnected()}}")
            }

            post("/api/v1/auth/login") {
                val body = call.receiveText()
                val json = parseJson(body)
                val login = json["login"] ?: ""
                val password = json["password"] ?: ""
                val deviceId = json["device_id"] ?: "unknown"
                val deviceName = json["device_name"] ?: "Unknown"

                if (login.isEmpty() || password.isEmpty()) {
                    call.respondText("{\"error\":\"Missing login or password\"}", status = HttpStatusCode.BadRequest)
                    return@post
                }

                val result = AuthService.login(login, password, deviceId, deviceName)
                if (result.isFailure) {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    val status = if (error.contains("not found")) HttpStatusCode.NotFound else HttpStatusCode.Unauthorized
                    call.respondText("{\"error\":\"$error\"}", status = status)
                    return@post
                }

                call.respondText("{\"token\":\"${result.getOrNull()}\",\"login\":\"$login\"}")
            }

            get("/api/v1/auth/verify") {
                val auth = call.request.headers["Authorization"]
                if (auth == null) {
                    call.respondText("{\"error\":\"Missing token\"}", status = HttpStatusCode.Unauthorized)
                    return@get
                }

                val result = AuthService.verify(auth)
                if (result.isFailure) {
                    call.respondText("{\"error\":\"${result.exceptionOrNull()?.message}\"}", status = HttpStatusCode.Unauthorized)
                    return@get
                }

                val device = result.getOrNull()
                call.respondText("{\"login\":\"${device?.login}\",\"device_name\":\"${device?.deviceName}\"}")
            }

            post("/api/v1/sync/push") {
                val auth = call.request.headers["Authorization"]
                if (auth == null) {
                    call.respondText("{\"error\":\"Missing token\"}", status = HttpStatusCode.Unauthorized)
                    return@post
                }

                val verifyResult = AuthService.verify(auth)
                if (verifyResult.isFailure) {
                    call.respondText("{\"error\":\"Invalid token\"}", status = HttpStatusCode.Unauthorized)
                    return@post
                }

                val body = call.receiveText()
                val request = parseSyncRequest(body)
                val response = SyncService.push(request)

                call.respondText("{\"success\":${response.success},\"timestamp\":${response.timestamp},\"synced\":${response.synced}}")
            }

            get("/api/v1/sync/changes") {
                val auth = call.request.headers["Authorization"]
                if (auth == null) {
                    call.respondText("{\"error\":\"Missing token\"}", status = HttpStatusCode.Unauthorized)
                    return@get
                }

                val verifyResult = AuthService.verify(auth)
                if (verifyResult.isFailure) {
                    call.respondText("{\"error\":\"Invalid token\"}", status = HttpStatusCode.Unauthorized)
                    return@get
                }

                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val response = SyncService.getChanges(since)

                val result = "{\"timestamp\":${response.timestamp},\"accounts\":[${response.accounts.joinToString(",")}],\"categories\":[${response.categories.joinToString(",")}],\"transactions\":[${response.transactions.joinToString(",")}]}"
                call.respondText(result)
            }

            get("/api/v1/sync/pull") {
                val auth = call.request.headers["Authorization"]
                if (auth == null) {
                    call.respondText("{\"error\":\"Missing token\"}", status = HttpStatusCode.Unauthorized)
                    return@get
                }

                val verifyResult = AuthService.verify(auth)
                if (verifyResult.isFailure) {
                    call.respondText("{\"error\":\"Invalid token\"}", status = HttpStatusCode.Unauthorized)
                    return@get
                }

                val response = SyncService.pull()

                val result = "{\"timestamp\":${response.timestamp},\"accounts\":[${response.accounts.joinToString(",")}],\"categories\":[${response.categories.joinToString(",")}],\"transactions\":[${response.transactions.joinToString(",")}]}"
                call.respondText(result)
            }
        }
    }.start(wait = true)
}

fun parseJson(json: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val regex = """\"(\w+)\"\s*:\s*"([^"]*)"""".toRegex()
    regex.findAll(json).forEach { match ->
        val key = match.groupValues.getOrNull(1) ?: return@forEach
        val value = match.groupValues.getOrNull(2) ?: ""
        result[key] = value
    }
    return result
}

fun parseSyncRequest(json: String): SyncRequest {
    val accounts = extractArray(json, "accounts").map { parseJson(it) }
    val categories = extractArray(json, "categories").map { parseJson(it) }
    val transactions = extractArray(json, "transactions").map { parseJson(it) }
    return SyncRequest(accounts, categories, transactions)
}

fun extractArray(json: String, key: String): List<String> {
    val pattern = "$key\\s*:\\s*\\[([^\\]]*)\\]".toRegex()
    val match = pattern.find(json) ?: return emptyList()
    val content = match.groupValues[1].trim()
    if (content.isEmpty()) return emptyList()
    val items = mutableListOf<String>()
    var depth = 0
    val current = StringBuilder()
    for (c in content) {
        if (c == '{') depth++
        if (c == '}') depth--
        if (c == ',' && depth == 0) {
            items.add(current.toString().trim())
            current.clear()
        } else {
            current.append(c)
        }
    }
    if (current.isNotBlank()) items.add(current.toString().trim())
    return items
}