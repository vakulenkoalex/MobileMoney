pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.jvm") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:2.0.21")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mobilemoney-server"
rootProject.projectDir = file(".")
includeBuild("../common") {
    dependencySubstitution {
        substitute(module("com.mobilemoney:common")).using(project(":"))
    }
}