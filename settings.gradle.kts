// File: settings.gradle.kts

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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    // V-- НАЧАЛО ИЗМЕНЕНИЙ: Ослабляем строгость правила --V
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Было: FAIL_ON_PROJECT_REPOS
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
    repositories {
        google()
        mavenCentral()
        // Talsec repository for freeRASP
        maven { url = uri("https://developer.talsec.app/repository/release") }
        // Добавляем репозиторий, где лежат графики YCharts и Vico
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QrScannerApp"
include(":app")