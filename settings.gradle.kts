rootProject.name = "Pixiv-MultiPlatform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("plugin/compose-desktop-build-windows")
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        //FIXME: 当且仅当在macOS上工作。在正式发布版本时需要删掉
        mavenLocal {
            url = uri("file:///Users/sheng233/.m2/repository")
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://jogamp.org/deployment/maven")
    }
}

include(":composeApp")

//some API libraries was in here
//sometimes it can't publish
//other is fork from others
include(":lib:chip-text-field")
include(":lib:epub")
include(":lib:okio-enhancement-util")
include(":lib:gif")
include(":lib:sketch-fetcher-file-adapter-windows")
//include(":plugin:compose-desktop-build-windows")
