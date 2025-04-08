import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.source

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
        ndk {
            // 只支持arm64和x64
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    externalNativeBuild {
        cmake {
            path = File("src/rust/CMakeLists.txt")
        }
    }
}

val kotlinArchToRustArch = mapOf(
    "iosX64" to "x86_64-apple-ios",
    "iosArm64" to "aarch64-apple-ios",
    "iosSimulatorArm64" to "aarch64-apple-ios-sim",
)

kotlin {
    jvmToolchain(17)
    jvm()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { t ->
        t.apply {
            compilations.all {
                cinterops {
                    val gif by creating {
                        defFile("src/iosMain/interop/libgif_rust.def")
                        packageName("moe.tarsin.gif.cinterop")
                        includeDirs("src/iosMain/interop/include")
                    }
                }
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.cbor)
            implementation(project(":lib:okio-enhancement-util"))
        }
    }
}

enum class JvmDesktopPlatform {
    WINDOWS,
    LINUX,
    MACOS,
}

val currentJvmPlatform by lazy {
    val prop = System.getProperty("os.name")
    when {
        prop.startsWith("Mac") -> JvmDesktopPlatform.MACOS
        prop.startsWith("Linux") -> JvmDesktopPlatform.LINUX
        prop.startsWith("Win") -> JvmDesktopPlatform.WINDOWS
        else -> error("unsupported platform: $prop")
    }
}

val jvmPlatformLibraryName by lazy {
    when (currentJvmPlatform) {
        JvmDesktopPlatform.MACOS -> "libgif_rust.dylib"
        JvmDesktopPlatform.LINUX -> "libgif_rust.so"
        JvmDesktopPlatform.WINDOWS -> "gif_rust.dll"
    }
}

val jvmCargoBuildRelease = tasks.register<Exec>("jvmCargoBuildRelease") {
    val cmd = "cargo build --release --features jvm"
    workingDir = project.file("src/rust")
    when (currentJvmPlatform) {
        JvmDesktopPlatform.WINDOWS -> commandLine("cmd", "/c", cmd)
        JvmDesktopPlatform.LINUX, JvmDesktopPlatform.MACOS -> commandLine("bash", "-c", cmd)
    }
}

fun File.md5() = source().buffer().use { src ->
    HashingSink.md5(blackholeSink()).use { dst ->
        src.readAll(dst)
        dst.hash.hex().lowercase()
    }
}

val jvmMetadataGenerated = tasks.register("jvmMetadataGenerated") {
    dependsOn(jvmCargoBuildRelease)
    doFirst {
        val hash = project.file("src/rust/target/release/$jvmPlatformLibraryName").md5()
        project.file("src/rust/target/release/gif-build.hash").writeText(hash)
        logger.lifecycle("rust lib hash is $hash")
    }
}

tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(jvmMetadataGenerated)
    from(
        project.file("src/rust/target/release/$jvmPlatformLibraryName"),
        project.file("src/rust/target/release/gif-build.hash"),
    )
}

for ((kotlinArch, rustArch) in kotlinArchToRustArch) {
    val iosNativeCargoTask = tasks.register<Exec>("${kotlinArch}NativeCargoTask") {
        onlyIf { System.getProperty("os.name").startsWith("Mac") }
        workingDir = project.file("src/rust")
        commandLine("bash", "-c", "cargo build --release --target $rustArch")
    }
    tasks.named("cinteropGif${kotlinArch.replaceFirstChar(Char::uppercaseChar)}") {
        dependsOn(iosNativeCargoTask)
    }
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
