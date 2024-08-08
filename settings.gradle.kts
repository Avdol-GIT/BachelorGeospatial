pluginManagement {
    repositories {
        //Aktivierung der Repository google()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        //Aktivierung der Repository google()
        google()
        mavenCentral()
    }
}

rootProject.name = "geospatialPrototyp"
include(":app")
 