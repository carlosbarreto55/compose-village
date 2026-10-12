rootProject.name = "compose-village"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":host-api", ":compiler", ":preview", ":levels", ":app")
