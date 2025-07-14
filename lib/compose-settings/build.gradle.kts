plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

group = "com.alorma.compose.settings"
version = "1.0"

fun prop(key: String) = project.findProperty(key) as String

android {
    namespace = "com.alorma.compose.settings"

    compileSdk = prop("TARGET_SDK").toInt()

    defaultConfig {
        minSdk = prop("MIN_SDK").toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin {
    jvmToolchain(17)

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(compose.ui)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
            }
        }
    }
}

