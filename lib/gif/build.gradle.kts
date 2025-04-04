import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import org.jetbrains.kotlin.daemon.common.toHexString

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.buildConfig)
}

group = "top.kagg886.gif"
version = "1.0"

buildConfig {
    packageName("top.kagg886.gif")
    buildConfigField("LIB_VERSION", UUID.randomUUID().toString().replace("-", ""))
}

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
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.cbor)
            implementation(project(":lib:okio-enhancement-util"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
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

val jvmCargoBuildRelease = tasks.register<Exec>("jvmCargoBuildRelease") {
    val cmd = "cargo build --release --features jvm"
    workingDir = project.file("src/rust")
    if (System.getProperty("os.name").startsWith("Win")) {
        commandLine("cmd", "/c", cmd) // windows should use cmdlet
        return@register
    }
    commandLine("bash", "-c", cmd)
}

val jvmMetadataGenerated = tasks.register("jvmMetadataGenerated") {
    dependsOn(jvmCargoBuildRelease)

    fun InputStream.md5(): String {
        val md5 = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(1024)
        var len: Int
        while (read(buffer).also { len = it } != -1) {
            md5.update(buffer, 0, len)
        }
        return md5.digest().toHexString()
    }

    doFirst {
        val libName = when (currentJvmPlatform) {
            JvmDesktopPlatform.MACOS -> "libgif_rust.dylib"
            JvmDesktopPlatform.LINUX -> "libgif_rust.so"
            JvmDesktopPlatform.WINDOWS -> "gif_rust.dll"
        }

        val hash = project.file("src/rust/target/release/$libName").inputStream().md5()
        project.file("src/rust/target/release/gif-build.hash").writeText(hash)
        logger.lifecycle("rust lib hash is $hash")
    }
}

tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(jvmMetadataGenerated)
    val libName = when (currentJvmPlatform) {
        JvmDesktopPlatform.MACOS -> "libgif_rust.dylib"
        JvmDesktopPlatform.LINUX -> "libgif_rust.so"
        JvmDesktopPlatform.WINDOWS -> "gif_rust.dll"
    }
    from(project.file("src/rust/target/release/$libName"), project.file("src/rust/target/release/gif-build.hash"))
}

// val linuxNativeCargoTask = tasks.register<Exec>("linuxNativeCargoTask") {
//    onlyIf { System.getProperty("os.name").startsWith("Linux") }
//    workingDir = project.file("src/rust")
//    commandLine("bash", "-c", "cargo build --release")
// }
//
// tasks.named<ProcessResources>("linuxX64ProcessResources") {
//    dependsOn(linuxNativeCargoTask)
// }

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
