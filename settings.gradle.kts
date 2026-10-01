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

rootProject.name = "Memento"
include(":app")

// Core modules
include(":core:common")
include(":core:database")
include(":core:network")
include(":core:media")
include(":core:designsystem")
include(":core:ui")
include(":core:domain")

// Feature modules
include(":feature:auth:domain")
include(":feature:auth:data")
include(":feature:auth:presentation")

include(":feature:home:domain")
include(":feature:home:data")
include(":feature:home:presentation")

include(":feature:connection:domain")
include(":feature:connection:data")
include(":feature:connection:presentation")

include(":feature:post:domain")
include(":feature:post:data")
include(":feature:post:presentation")
