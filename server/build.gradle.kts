plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("application")
}

group = "com.mobilemoney"
version = "1.0.0"

application {
    mainClass.set("com.mobilemoney.server.ApplicationKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.mobilemoney.server.ApplicationKt"
    }
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
    implementation("io.ktor:ktor-server-core:3.4.3")
    implementation("io.ktor:ktor-server-netty:3.4.3")
    implementation("io.ktor:ktor-server-call-logging:3.4.3")
    implementation("io.ktor:ktor-server-status-pages:3.4.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")

    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(project(":common"))

    testImplementation(kotlin("test"))
}