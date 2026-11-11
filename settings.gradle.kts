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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Local AAR directory for Hyundai Pleos Connect SDK drop-in (.aar/.jar)
        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "DriveCheck"
include(":app")
