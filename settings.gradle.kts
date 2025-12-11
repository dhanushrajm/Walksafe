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
        // --- THIS IS THE MISSING LINE ---
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Walksafe"
include(":app")