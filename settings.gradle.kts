pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "brev"

include(
    "brev-core",
    "brev-documents",
    "brev-smp",
    "brev-ap",
    "benchmark",
    "conformance"
)
