plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.benmanes.versions)
    alias(libs.plugins.version.catalog.update)
}

allprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("androidx.sqlite:sqlite:2.5.2")
        }
    }
}
