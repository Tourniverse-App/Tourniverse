pluginManagement {
    repositories {
        gradlePluginPortal()
        google() // Android-specific plugins
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.8.0" // Adjust to your Android Gradle plugin version
        id("androidx.navigation.safeargs.kotlin") version "2.8.4" // Adjust the version to match your needs
        kotlin("android") version "1.9.0" // Ensure Kotlin version matches the project's requirements
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
