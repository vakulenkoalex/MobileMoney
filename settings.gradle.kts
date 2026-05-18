pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.jvm") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:2.3.21")
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

rootProject.name = "MobileMoney"
include(":android")
include(":common")
include(":server")