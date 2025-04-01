import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

fun prop(key: String) = project.findProperty(key) as String

val pkgName: String = "top.kagg886.pmf"

// val pkgVersion: String = "1.0.0"
// val pkgCode: Int = 1

val appVersionName = System.getenv("APP_VERSION_NAME") ?: prop("APP_VERSION_NAME")
check(appVersionName.startsWith("v")) { "APP_VERSION not supported, current is $appVersionName" }
val pkgVersion: String = appVersionName.substring(1)
val pkgCode: Int = with(pkgVersion.split(".")) {
    val x = this[0].toInt()
    val y = this[1].toInt()
    val z = this[2].toInt()
    x * 100 + y * 10 + z
}

println("APP_VERSION: $pkgVersion($pkgCode)")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.room)
    alias(libs.plugins.aboutlibrariesPlugin)
    id("top.kagg886.compose.installer.windows")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    ksp(libs.androidx.room.compiler)
}

buildConfig {
    packageName(pkgName)
    buildConfigField("APP_NAME", rootProject.name)
    buildConfigField("APP_BASE_PACKAGE", pkgName)
    buildConfigField("APP_VERSION_NAME", pkgVersion)
    buildConfigField("APP_VERSION_CODE", pkgCode)

    buildConfigField("DATABASE_VERSION", 6)
}
kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm("desktop")

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts.add("-lsqlite3")
        }
    }

    sourceSets.named("commonMain").configure {
//        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
    sourceSets {
        commonMain.dependencies {
            // kotlin stdlib
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            // compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // voyager
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.bottom.sheet.navigator)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.transitions)
            implementation(libs.koin.core)

            // orbit
            implementation(libs.orbit.core)

            // pixiv
            implementation(libs.pixko)

            // storage
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.serialization)

            // settings-ui
            implementation(libs.compose.settings.ui)
            implementation(libs.compose.settings.extended)

            // search-page-ui
            implementation(project(":lib:chip-text-field"))

            // webview
            api(libs.compose.webview.multiplatform)

            // image-loader
            implementation(libs.sketch.compose)
            implementation(libs.sketch.svg)
            implementation(libs.sketch.resources)
            implementation(libs.sketch.animated.gif)
            implementation(libs.sketch.extensions.compose)
            implementation(libs.sketch.http.ktor3)
            implementation(libs.zoomimage.compose.sketch)

            // gif-exporter
            implementation(project(":lib:gif"))

            // ktor
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // room
            implementation(libs.androidx.room.runtime)

            // save file to storage
            implementation(libs.filekit.compose)

            // logging
            implementation(libs.kermit)

            // epub module
            implementation(project(":lib:epub"))

            implementation(project(":lib:okio-enhancement-util"))

            // zip files
            implementation(libs.korlibs.io)
            // use to HTML parse
            implementation(libs.ksoup)

            // about page
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.sqlite.bundled)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.androidx.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

aboutLibraries {
    duplicationMode = MERGE
    duplicationRule = GROUP
}

android {
    namespace = pkgName
    compileSdk = prop("TARGET_SDK").toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
//    sourceSets["main"].res.srcDirs("src/androidMain/res", "src/commonMain/composeResources")

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    signingConfigs {
        create("test") {
            storeFile = file("app.jks")
            storePassword = "123456"

            keyAlias = "kagg886"
            keyPassword = "123456"
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    defaultConfig {
        applicationId = pkgName
        minSdk = prop("MIN_SDK").toInt()
        targetSdk = prop("TARGET_SDK").toInt()
        versionCode = pkgCode
        versionName = pkgVersion

        manifestPlaceholders["APP_NAME"] = rootProject.name

        ndk {
            // 只支持arm64和x64
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        val signConfig = signingConfigs.getByName("test")

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signConfig
        }

        getByName("debug") {
            signingConfig = signConfig
            manifestPlaceholders["APP_NAME"] = "${rootProject.name} (Debug)"
            applicationIdSuffix = ".debug"
        }
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
compose.resources {
    publicResClass = false
    packageOfResClass = pkgName
    generateResClass = auto
}
compose.desktop {
    application {
        mainClass = "$pkgName.MainKt"
        nativeDistributions {
            includeAllModules = true
            targetFormats(
                *buildList {
                    add(TargetFormat.Msi)
                    add(TargetFormat.Dmg)
                    if (!System.getProperty("os.name").contains("Mac")) {
                        add(TargetFormat.AppImage)
                    }
                }.toTypedArray(),
            )
            packageName = rootProject.name
            packageVersion = pkgVersion

            windows {
                iconFile.set(project.file("icons/pixiv.ico"))
            }

            linux {
                iconFile.set(project.file("icons/pixiv.png"))
            }

            macOS {
                iconFile.set(project.file("icons/pixiv.icns"))
            }
        }

        buildTypes.release.proguard {
            isEnabled = false
        }

        // release mode
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
        afterEvaluate {
            tasks.withType<JavaExec> {
                // debug mode
                jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
                jvmArgs(
                    "--add-opens",
                    "java.desktop/java.awt.peer=ALL-UNNAMED",
                )

                if (System.getProperty("os.name").contains("Mac")) {
                    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
                    jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
                    jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
                }
            }
        }
    }
}

configureComposeWindowsInstaller {
    appName = rootProject.name
    appVersion = pkgVersion
    shortcutName = rootProject.name
    iconFile = project.file("icons/pixiv.ico")
    manufacturer = "kagg886"
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
