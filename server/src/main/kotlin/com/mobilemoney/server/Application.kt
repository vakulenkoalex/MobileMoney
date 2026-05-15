package com.mobilemoney.server

import com.mobilemoney.server.repository.Database
import com.mobilemoney.server.route.authRoutes
import com.mobilemoney.server.route.healthRoutes
import com.mobilemoney.server.route.syncRoutes
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main() {
    val nettyPort = (System.getenv("NETTY_PORT") ?: "6080").toInt()

    val dbInitialized = Database.init()
    if (dbInitialized) {
        println("Database initialized: sync.db")
    }

    embeddedServer(Netty, port = nettyPort) {
        routing {
            healthRoutes()
            authRoutes()
            syncRoutes()
        }
    }.start(wait = true)
}