plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.2"
}

group = "com.mobilemoney"
version = "1.0.0"

application {
    mainClass.set("com.mobilemoney.server.ApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.0.2")
    implementation("io.ktor:ktor-server-netty:3.0.2")
    implementation("io.ktor:ktor-server-call-logging:3.0.2")
    implementation("io.ktor:ktor-server-status-pages:3.0.2")

    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}