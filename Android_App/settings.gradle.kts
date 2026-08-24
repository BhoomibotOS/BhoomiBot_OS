pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BhoomiBot_OS"
include(":app")

// AI-Fix: Link Standalone Brain Core from D:/Bhoomibot_OS/Repository/AI
includeBuild("../AI") {
    dependencySubstitution {
        substitute(module("com.bhoomibot:ai-core")).using(project(":core"))
        substitute(module("com.bhoomibot:ai-sdk")).using(project(":sdk"))
    }
}