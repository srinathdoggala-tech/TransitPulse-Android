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

rootProject.name = "habittracker"
include(":app")
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:firebase")
include(":core:notifications")
include(":core:designsystem")
include(":domain")
include(":feature:onboarding")
include(":feature:habits")
include(":feature:settings")
