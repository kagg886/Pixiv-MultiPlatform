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

    listOf(iosX64(), iosArm64(), iosSimulatorArm64(), linuxX64()).forEach { t ->
        t.apply {
            compilations.all {
                cinterops {
                    val gif by creating {
                        defFile("src/nativeMain/interop/libgif_rust.def")
                        packageName("moe.tarsin.gif.cinterop")
                        includeDirs("src/nativeMain/interop/include")
                        extraOpts("-libraryPath", "src/rust/target/release")
                    }
                }
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kermit)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.okio)
            }
        }

        linuxX64Main.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val jvmNonAndroidNatveLibBuild = tasks.register<Exec>("jvmNonAndroidNatveLibBuild") {
    onlyIf { System.getProperty("os.name").startsWith("Linux") }
    workingDir = project.file("src/rust")
    commandLine("bash", "-c", "cargo build --release --features jvm")
}

tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(jvmNonAndroidNatveLibBuild)
    from(project.file("src/rust/target/release/libgif_rust.so"))
}

val linuxNativeRustBuild = tasks.register<Exec>("linuxNativeRustBuild") {
    onlyIf { System.getProperty("os.name").startsWith("Linux") }
    workingDir = project.file("src/rust")
    commandLine("bash", "-c", "cargo build")
}

tasks.named<ProcessResources>("linuxX64ProcessResources") {
    dependsOn(linuxNativeRustBuild)
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
