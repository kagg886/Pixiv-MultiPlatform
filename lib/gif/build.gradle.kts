plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spotless)
}

group = "top.kagg886.gif"
version = "1.0"

fun prop(key: String) = project.findProperty(key) as String

android {
    ndkVersion = "28.0.13004108"
    namespace = "top.kagg886.gif"

    compileSdk = prop("TARGET_SDK").toInt()

    defaultConfig {
        minSdk = prop("MIN_SDK").toInt()
        externalNativeBuild {
            cmake {
                targets += "cargo-build_gif_rust"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        cmake {
            path = File("src/rust/CMakeLists.txt")
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
                implementation(libs.kermit)
                implementation(libs.pixko)
                implementation(libs.kotlinx.serialization.cbor)
            }
        }
    }
}

val jvmNonAndroidNatveLibBuild = tasks.register<Exec>("jvmNonAndroidNatveLibBuild") {
    onlyIf { System.getProperty("os.name").startsWith("Linux") }
    workingDir = project.file("src/rust")
    commandLine("bash", "-c", "cargo build --release --features jvm")
}

// 配置JVM的processResources任务
tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(jvmNonAndroidNatveLibBuild)
    from(project.file("src/rust/target/release/libgif_rust.so"))
}

val ktlintVersion = libs.ktlint.get().version

spotless {
    kotlin {
        // https://github.com/diffplug/spotless/issues/111
        target("src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        ktlint(ktlintVersion)
    }
}
