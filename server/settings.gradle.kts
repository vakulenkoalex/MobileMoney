pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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