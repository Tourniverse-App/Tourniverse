pluginManagement {
    repositories {
        gradlePluginPortal()
        google() // Android-specific plugins
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.8.0"
        id("androidx.navigation.safeargs.kotlin") version "2.8.4"
        kotlin("android") version "1.9.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tourniverse"
include(":app")
