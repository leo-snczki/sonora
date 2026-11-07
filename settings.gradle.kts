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
    }
}

rootProject.name = "sonora"

include(
    ":app",
    ":core:domain",
    ":core:designsystem",
    ":core:data",
    ":core:media",
    ":core:render",
    ":feature:backdrops",
    ":feature:home",
    ":wallpaper",
)
