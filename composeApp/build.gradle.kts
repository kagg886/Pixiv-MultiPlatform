import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractProguardTask

fun prop(key: String) = project.findProperty(key) as String

val pkgName: String = "top.kagg886.pmf"

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
    buildConfigField("APP_COMMIT_ID", getGitSha())
}
kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm("desktop")

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts += "-lsqlite3"
        }
    }

    compilerOptions {
        progressiveMode = true
        optIn.addAll(
            "coil3.annotation.ExperimentalCoilApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.animation.ExperimentalSharedTransitionApi",
            "kotlin.ExperimentalStdlibApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlin.contracts.ExperimentalContracts",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "org.jetbrains.compose.resources.ExperimentalResourceApi",
        )
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
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
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.paging)

            // voyager
            implementation(libs.voyager.navigator)
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
            implementation(project(":lib:compose-settings"))
//            implementation(libs.compose.settings.ui)
//            implementation(libs.compose.settings.extended)

            // search-page-ui
            implementation(project(":lib:chip-text-field"))

            // webview
            api(libs.compose.webview.multiplatform)

            // https://coil-kt.github.io/coil/changelog/
            implementation(dependencies.platform(libs.coil.bom))
            implementation(libs.bundles.coil)
            implementation(libs.telephoto.zoomable)

            // gif-exporter
            implementation(project(":lib:gif"))

            // ktor
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // room
            implementation(libs.bundles.room)

            // save file to storage
            implementation(project(":lib:file-picker"))
//            implementation(libs.filekit.compose)
//            implementation(libs.filekit.core)

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

            implementation(project.dependencies.platform(libs.arrow.stack))
            implementation(libs.arrow.fx.coroutines)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.java)
            implementation(libs.androidx.sqlite.bundled)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.coil.gif)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation("com.russhwolf:multiplatform-settings-test:1.3.0")
            implementation(kotlin("test"))
        }
    }
}

aboutLibraries.library {
    duplicationMode = MERGE
    duplicationRule = GROUP
}

android {
    namespace = pkgName
    compileSdk = prop("TARGET_SDK").toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

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
            proguardFiles("core-rules.pro")
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
    publicResClass = true
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
                    if ("Mac" !in System.getProperty("os.name")) {
                        add(TargetFormat.AppImage)
                    }
                }.toTypedArray(),
            )
            packageName = rootProject.name
            packageVersion = pkgVersion

            windows { iconFile.set(file("icons/pixiv.ico")) }
            linux { iconFile.set(file("icons/pixiv.png")) }
            macOS { iconFile.set(file("icons/pixiv.icns")) }
        }

        // release mode
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if ("Mac" in System.getProperty("os.name")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
        afterEvaluate {
            tasks.withType<JavaExec> {
                // debug mode
                jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
                jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

                if ("Mac" in System.getProperty("os.name")) {
                    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
                    jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
                    jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
                }
            }
        }
    }
}

tasks.withType(AbstractProguardTask::class.java) {
    val proguardFile = File.createTempFile("tmp", ".pro", temporaryDir)
    proguardFile.deleteOnExit()

    compose.desktop.application.buildTypes.release.proguard {
        configurationFiles.from(proguardFile, file("core-rules.pro"), file("desktop-rules.pro"))
        optimize = false // fixme(tarsin): proguard internal error
        obfuscate = true
        joinOutputJars = true
    }

    doFirst {
        proguardFile.bufferedWriter().use { proguardFileWriter ->
            sourceSets["desktopMain"].runtimeClasspath.filter { it.extension == "jar" }.forEach { jar ->
                val zip = zipTree(jar)
                zip.matching { include("META-INF/**/proguard/*.pro") }.forEach {
                    proguardFileWriter.appendLine("########   ${jar.name} ${it.name}")
                    proguardFileWriter.appendLine(it.readText())
                }
                zip.matching { include("META-INF/services/*") }.forEach {
                    it.readLines().forEach { cls ->
                        val rule = "-keep class $cls"
                        proguardFileWriter.appendLine(rule)
                    }
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

fun getGitSha(): String {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .redirectErrorStream(true)
        .start()
    val rtn = process.inputStream.readAllBytes().decodeToString().trim()
    check(rtn.length == 7) {
        throw IllegalStateException("git rev-parse failed, rtn:\n$rtn")
    }
    return rtn
}

// ------------------ IOS Packages Build ------------------

// context path is rootProject.dir("iosApp")
fun ipaArguments(
    destination: String = "generic/platform=iOS",
    sdk: String = "iphoneos",
): Array<String> = arrayOf(
    "xcodebuild",
    "-project", "Pixiv-MultiPlatform.xcodeproj",
    "-scheme", "Pixiv-MultiPlatform",
    "-destination", destination,
    "-sdk", sdk,
    "CODE_SIGNING_ALLOWED=NO",
    "CODE_SIGNING_REQUIRED=NO",
)

val buildReleaseArchive = tasks.register("buildReleaseArchive", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Release"
    workingDir(rootProject.file("iosApp"))

    val output = layout.buildDirectory.dir("archives/release/Pixiv-MultiPlatform.xcarchive")
    outputs.dir(output)
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration",
        "Release",
        "-archivePath",
        output.get().asFile.absolutePath,
    )
}

tasks.register("buildReleaseIpa", BuildIpaTask::class) {
    description = "Manually packages the .app from the .xcarchive into an unsigned .ipa"
    group = "build"

    // Adjust these paths as needed
    archiveDir = layout.buildDirectory.dir("archives/release/Pixiv-MultiPlatform.xcarchive")
    outputIpa = layout.buildDirectory.file("archives/release/Pixiv-MultiPlatform.ipa")
    dependsOn(buildReleaseArchive)
}

@CacheableTask
abstract class BuildIpaTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    abstract val archiveDir: DirectoryProperty

    @get:OutputFile
    abstract val outputIpa: RegularFileProperty

    @TaskAction
    fun buildIpa() {
        // 1. Locate the .app in the .xcarchive
        val appDir = archiveDir.get().asFile.resolve("Products/Applications/Pixiv-MultiPlatform.app")
        if (!appDir.exists()) {
            throw GradleException("Could not find Pixiv-MultiPlatform.app in archive at: ${appDir.absolutePath}")
        }

        // 2. Create a temporary Payload folder
        val payloadDir = File(temporaryDir, "Payload").apply { mkdirs() }
        val destApp = File(payloadDir, "Pixiv-MultiPlatform.app")

        // 3. Copy the .app into Payload/
        appDir.copyRecursively(destApp, overwrite = true)

        // 4. Zip the Payload folder
        val zipFile = File(temporaryDir, "Pixiv-MultiPlatform.zip")
        zipDirectory(payloadDir, zipFile)

        // 5. Rename .zip to .ipa
        val ipaFile = outputIpa.get().asFile
        ipaFile.parentFile.mkdirs()
        if (ipaFile.exists()) ipaFile.delete()
        zipFile.renameTo(ipaFile)

        logger.lifecycle("Created unsigned IPA at: ${ipaFile.absolutePath}")
    }

    /**
     * Zips the given [sourceDir] (including all subdirectories) into [outputFile].
     */
    private fun zipDirectory(sourceDir: File, outputFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(sourceDir.parentFile).path
                    val zipEntry = ZipEntry(relativePath)
                    zipOut.putNextEntry(zipEntry)
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
    }
}
