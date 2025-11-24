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
dependencyResolutionManagement {
    // V-- НАЧАЛО ИЗМЕНЕНИЙ: Ослабляем строгость правила --V
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Было: FAIL_ON_PROJECT_REPOS
    // ^-- КОНЕЦ ИЗМЕНЕНИЙ --^
    repositories {
        google()
        mavenCentral()
        // Добавляем репозиторий, где лежат графики YCharts и Vico
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QrScannerApp"
include(":app")