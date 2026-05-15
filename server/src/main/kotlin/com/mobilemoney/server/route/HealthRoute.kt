package com.mobilemoney.server.route

import com.mobilemoney.server.repository.Database
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.healthRoutes() {
    get("/") {
        val dbConnected = Database.isConnected()
        call.respondText("{\"status\":\"ok\",\"database\":$dbConnected}")
    }
}