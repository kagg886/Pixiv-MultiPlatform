plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

group = "top.kagg886.gif"
version = "1.0"

fun prop(key: String) = project.findProperty(key) as String

android {
    namespace = "top.kagg886.gif"

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
                implementation(project(":lib:okio-enhancement-util"))
                api(libs.korlibs.io)
                implementation(libs.kermit)
            }
        }

        jvmTest.dependencies {
            val osName = System.getProperty("os.name")
            val targetOs = when {
                osName == "Mac OS X" -> "macos"
                osName.startsWith("Win") -> "windows"
                osName.startsWith("Linux") -> "linux"
                else -> error("Unsupported OS: $osName")
            }

            val targetArch = when (val osArch = System.getProperty("os.arch")) {
                "x86_64", "amd64" -> "x64"
                "aarch64" -> "arm64"
                else -> error("Unsupported arch: $osArch")
            }

            val version = "0.8.9" // or any more recent version
            val target = "${targetOs}-${targetArch}"
            //noinspection UseTomlInstead
            implementation("org.jetbrains.skiko:skiko-awt-runtime-$target:$version")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

