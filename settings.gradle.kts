pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("pangLibs") {
            from(files("gradle/pang-libs.versions.toml"))
        }
    }
}

rootProject.name = "pang-image"
include(":app")
