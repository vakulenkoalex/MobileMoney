plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("application")
}

group = "com.mobilemoney"


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
    implementation("io.ktor:ktor-server-core:3.5.0")
    implementation("io.ktor:ktor-server-netty:3.5.0")
    implementation("io.ktor:ktor-server-call-logging:3.5.0")
    implementation("io.ktor:ktor-server-status-pages:3.5.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.5.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.0")

    implementation("org.xerial:sqlite-jdbc:3.53.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(project(":common"))

    testImplementation(kotlin("test"))
}

tasks.processResources {
    expand(properties)
}