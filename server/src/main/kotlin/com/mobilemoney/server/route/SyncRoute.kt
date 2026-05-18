package com.mobilemoney.server.route

import com.mobilemoney.dto.SyncChangesResponse
import com.mobilemoney.dto.SyncPushRequest
import com.mobilemoney.dto.SyncPushResponse
import com.mobilemoney.server.DI
import com.mobilemoney.server.service.AuthService
import com.mobilemoney.server.service.SyncService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Routing.syncRoutes() {
    val authService: AuthService = DI.authService
    val syncService: SyncService = DI.syncService

    post("/api/v1/sync/push") {
        val auth = call.request.headers["Authorization"]
        if (auth == null) {
            call.respondText("{\"error\":\"Missing token\"}", status = HttpStatusCode.Unauthorized)
            return@post
        }

        val verifyResult = authService.verify(auth)
        if (verifyResult.isFailure) {
            call.respondText("{\"error\":\"Invalid or expired token\",\"code\":\"AUTH_FAILED\"}", status = HttpStatusCode.Unauthorized)
            return@post
        }

        val body = call.receiveText()
        try {
            val request = Json.decodeFromString<SyncPushRequest>(body)
            val response = syncService.push(request)
            val json = Json.encodeToString(SyncPushResponse.serializer(), response)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            println("ERROR in push: ${e.message}")
            e.printStackTrace()
            call.respondText("{\"error\":\"Internal server error: ${e.message}\"}", status = HttpStatusCode.InternalServerError)
        }
    }

    get("/api/v1/sync/changes") {
        val auth = call.request.headers["Authorization"]
        if (auth == null) {
            call.respondText("{\"error\":\"Missing token\"}", status = HttpStatusCode.Unauthorized)
            return@get
        }

        val verifyResult = authService.verify(auth)
        if (verifyResult.isFailure) {
            call.respondText("{\"error\":\"Invalid or expired token\",\"code\":\"AUTH_FAILED\"}", status = HttpStatusCode.Unauthorized)
            return@get
        }

        val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
        try {
            val response = syncService.getChanges(since)
            val json = Json.encodeToString(SyncChangesResponse.serializer(), response)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            println("ERROR in getChanges: ${e.message}")
            e.printStackTrace()
            call.respondText("{\"error\":\"Internal server error: ${e.message}\"}", status = HttpStatusCode.InternalServerError)
        }
    }

    get("/api/v1/sync/pull") {
        val auth = call.request.headers["Authorization"]
        if (auth == null) {
            call.respondText("{\"error\":\"Missing token\"}", status = HttpStatusCode.Unauthorized)
            return@get
        }

        val verifyResult = authService.verify(auth)
        if (verifyResult.isFailure) {
            call.respondText("{\"error\":\"Invalid or expired token\",\"code\":\"AUTH_FAILED\"}", status = HttpStatusCode.Unauthorized)
            return@get
        }

        try {
            val response = syncService.pull()
            val json = Json.encodeToString(SyncChangesResponse.serializer(), response)
            call.respondText(json, contentType = ContentType.Application.Json)
        } catch (e: Exception) {
            println("ERROR in pull: ${e.message}")
            e.printStackTrace()
            call.respondText("{\"error\":\"Internal server error: ${e.message}\"}", status = HttpStatusCode.InternalServerError)
        }
    }
}