plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
}

group = "top.kagg886.epub"
version = "1.0"

fun prop(key: String) = project.findProperty(key) as String

android {
    namespace = "top.kagg886.epub"

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
                //zip files
                implementation(project(":lib:okio-enhancement-util"))
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

