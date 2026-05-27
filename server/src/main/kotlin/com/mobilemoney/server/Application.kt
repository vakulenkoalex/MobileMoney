package com.mobilemoney.server

import com.mobilemoney.server.repository.Database
import com.mobilemoney.server.route.authRoutes
import com.mobilemoney.server.route.healthRoutes
import com.mobilemoney.server.route.syncRoutes
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import java.util.Properties

fun main() {
    val nettyPort = (System.getenv("NETTY_PORT") ?: "6080").toInt()

    val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("version.properties")
    val serverVersion = if (resourceStream != null) {
        resourceStream.use {
            val props = Properties()
            props.load(it.reader())
            props.getProperty("version", "unknown")
        }
    } else "unknown"
    println("Server version: $serverVersion")

    Database.init()
    
    embeddedServer(Netty, port = nettyPort) {
        routing {
            healthRoutes()
            authRoutes()
            syncRoutes()
        }
    }.start(wait = true)
}