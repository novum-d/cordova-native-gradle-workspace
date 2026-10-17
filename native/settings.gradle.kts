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
        // Used only by the optional catalog plugin resolution verification.
        maven("https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "CordovaNativeLibraries"
include(":feature", ":data")
// libs is imported automatically from gradle/libs.versions.toml by Gradle.
