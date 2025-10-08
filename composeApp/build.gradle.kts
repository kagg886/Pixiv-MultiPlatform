@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractProguardTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.withIndent
import java.nio.file.Path
import java.util.*
import kotlin.io.path.invariantSeparatorsPathString
import org.jetbrains.compose.internal.ideaIsInSyncProvider

fun prop(key: String) = (project.findProperty(key) as String?) ?: ""

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

val gitSha = run {
    val origin = System.getenv("APP_COMMIT_ID") ?: prop("APP_COMMIT_ID")
    if (origin.length != 6) {
        getGitHeaderCommitIdShort().apply {
            println("APP_COMMIT_ID not set, use system default.($this)")
        }
    } else {
        origin
    }
}

val proguardEnable = (System.getenv("PROGUARD_ENABLE") ?: prop("PROGUARD_ENABLE")).toBooleanStrictOrNull() ?: true

println("APP_VERSION: $pkgVersion($pkgCode)")
println("PROGUARD_ENABLE: $proguardEnable")
println("---- Java Info ----")
println("Java version: ${System.getProperty("java.version")}")
println("Java vendor:  ${System.getProperty("java.vendor")}")
println("Java home:    ${System.getProperty("java.home")}")
println("OS name:      ${System.getProperty("os.name")}")
println("OS arch:      ${System.getProperty("os.arch")}")
println("-------------------")

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

    buildConfigField("DATABASE_VERSION", 7)
    buildConfigField("APP_COMMIT_ID", gitSha)
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
            implementation(project(":lib:multiplatform-serializer-fix"))
//            implementation(libs.multiplatform.settings.serialization)

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
            implementation(libs.androidx.documentfile)
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
            isMinifyEnabled = proguardEnable
            isShrinkResources = proguardEnable
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
    packageOfResClass = "$pkgName.res"
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

if (proguardEnable) {
    /**
     * | 文件名                 | 主要内容       | 主要作用          |
     * | :------------------ | :--------- | :------------ |
     * | `configuration.txt` | 最终使用的混淆配置  | 调试规则是否合并正确    |
     * | `mapping.txt`       | 原名 → 混淆名映射 | 反混淆崩溃日志       |
     * | `resources.txt`     | 资源混淆映射     | 调试资源重命名       |
     * | `seeds.txt`         | 被保留的类      | 验证 `-keep` 结果 |
     * | `usage.txt`         | 被删除的类      | 检查优化结果、瘦身效果   |
     *
     */
    gradle.projectsEvaluated {
        tasks.named("proguardReleaseJars").configure {
            doFirst {
                layout.buildDirectory.file("compose/binaries/main-release/proguard").get().asFile.mkdirs()
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
} else {
    compose.desktop.application.buildTypes.release.proguard {
        isEnabled = false
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

fun getGitHeaderCommitIdShort(): String {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .redirectErrorStream(true)
        .start()
    val rtn = process.inputStream.readAllBytes().decodeToString().trim()
    check(rtn.length == 7) {
        throw IllegalStateException("git rev-parse failed, rtn:\n$rtn")
    }
    return rtn
}

// FIXME: 升级到compose 1.9.0后出错，需要添加这个fix。
tasks.withType(com.google.devtools.ksp.gradle.KspAATask::class.java).configureEach {
    if (name == "kspKotlinDesktop") {
        dependsOn(
            // compose-resources tasks for desktop and common
            "generateResourceAccessorsForDesktopMain",
            "generateActualResourceCollectorsForDesktopMain",
            "generateComposeResClass",
            "generateResourceAccessorsForCommonMain",
            "generateExpectResourceCollectorsForCommonMain",
            // buildConfig for non-Android targets
            "generateNonAndroidBuildConfig",
        )
    }

    if (name == "kspDebugKotlinAndroid") {
        dependsOn(
            // compose-resources tasks for android
            "generateResourceAccessorsForAndroidDebug",
            "generateResourceAccessorsForAndroidMain",
            "generateActualResourceCollectorsForAndroidMain",
            // common + res class
            "generateComposeResClass",
            "generateResourceAccessorsForCommonMain",
            "generateExpectResourceCollectorsForCommonMain",
            // buildConfig used by KSP inputs
            "generateNonAndroidBuildConfig",
        )
    }

    if (name == "kspReleaseKotlinAndroid") {
        dependsOn(
            // compose-resources tasks for android release
            "generateResourceAccessorsForAndroidRelease",
            "generateResourceAccessorsForAndroidMain",
            "generateActualResourceCollectorsForAndroidMain",
            // common + res class
            "generateComposeResClass",
            "generateResourceAccessorsForCommonMain",
            "generateExpectResourceCollectorsForCommonMain",
            // buildConfig used by KSP inputs
            "generateNonAndroidBuildConfig",
        )
    }

    // iOS targets
    if (name == "kspKotlinIosSimulatorArm64") {
        dependsOn(
            // compose-resources for ios sim and its hierarchy
            "generateResourceAccessorsForIosSimulatorArm64Main",
            "generateActualResourceCollectorsForIosSimulatorArm64Main",
            "generateResourceAccessorsForIosMain",
            "generateResourceAccessorsForAppleMain",
            "generateResourceAccessorsForNativeMain",
            // common + res class
            "generateComposeResClass",
            "generateResourceAccessorsForCommonMain",
            "generateExpectResourceCollectorsForCommonMain",
            // buildConfig used by KSP inputs
            "generateNonAndroidBuildConfig",
        )
    }

    if (name == "kspKotlinIosArm64") {
        dependsOn(
            "generateResourceAccessorsForIosArm64Main",
            "generateActualResourceCollectorsForIosArm64Main",
            "generateResourceAccessorsForIosMain",
            "generateResourceAccessorsForAppleMain",
            "generateResourceAccessorsForNativeMain",
            "generateComposeResClass",
            "generateResourceAccessorsForCommonMain",
            "generateExpectResourceCollectorsForCommonMain",
            "generateNonAndroidBuildConfig",
        )
    }

    if (name == "kspKotlinIosX64") {
        dependsOn(
            "generateResourceAccessorsForIosX64Main",
            "generateActualResourceCollectorsForIosX64Main",
            "generateResourceAccessorsForIosMain",
            "generateResourceAccessorsForAppleMain",
            "generateResourceAccessorsForNativeMain",
            "generateComposeResClass",
            "generateResourceAccessorsForCommonMain",
            "generateExpectResourceCollectorsForCommonMain",
            "generateNonAndroidBuildConfig",
        )
    }
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

//FIXME：修复可复现构建不一致的问题，CMP发版后删除后面的代码段

internal fun Project.ideaIsInSyncProvider(): Provider<Boolean> = provider {
    System.getProperty("idea.sync.active", "false").toBoolean()
}

abstract class IdeaImportTask : DefaultTask() {
    @get:Input
    val ideaIsInSync: Provider<Boolean> = project.ideaIsInSyncProvider()

    @TaskAction
    fun run() {
        try {
            safeAction()
        } catch (e: Exception) {
            //message must contain two ':' symbols to be parsed by IDE UI!
            logger.error("e: $name task was failed:", e)
            if (!ideaIsInSync.get()) throw e
        }
    }

    abstract fun safeAction()
}

tasks.named<org.jetbrains.compose.resources.GenerateActualResourceCollectorsTask>("generateActualResourceCollectorsForAndroidMain").configure {
    doLast {
        val kotlinDir = codeDir.get().asFile
        val inputDirs = resourceAccessorDirs.files

        logger.info("Clean directory $kotlinDir")
        kotlinDir.deleteRecursively()
        kotlinDir.mkdirs()

        val inputFiles = inputDirs.flatMap { dir ->
            dir.walkTopDown().filter { !it.isHidden && it.isFile && it.extension == "kt" }.toList()
        }
        logger.info("Generate actual ResourceCollectors for $kotlinDir")
        val funNames = inputFiles.mapNotNull { inputFile ->
            if (inputFile.nameWithoutExtension.contains('.')) {
                val (fileName, suffix) = inputFile.nameWithoutExtension.split('.')
                val type = ResourceType.values().sorted().firstOrNull { fileName.startsWith(it.accessorName, true) }
                val name = "_collect${suffix.uppercaseFirstChar()}${fileName}Resources"

                if (type == null) {
                    logger.warn("Unknown resources type: `$inputFile`")
                    null
                } else if (!inputFile.readText().contains(name)) {
                    logger.warn("A function '$name' is not found in the `$inputFile` file!")
                    null
                } else {
                    logger.info("Found collector function: `$name`")
                    type to name
                }
            } else {
                logger.warn("Unknown file name: `$inputFile`")
                null
            }
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sorted() }

        val pkgName = packageName.get()
        val resClassName = resClassName.get()
        val isPublic = makeAccessorsPublic.get()
        val useActual = useActualModifier.get()
        val spec = getActualResourceCollectorsFileSpec(
            packageName = pkgName,
            fileName = "ActualResourceCollectors",
            resClassName = resClassName,
            isPublic = isPublic,
            useActualModifier = useActual,
            typeToCollectorFunctions = funNames
        )
        spec.writeTo(kotlinDir)
    }
}

internal enum class ResourceType(val typeName: String, val accessorName: String) {
    DRAWABLE("drawable", "drawable"),
    STRING("string", "string"),
    STRING_ARRAY("string-array", "array"),
    PLURAL_STRING("plurals", "plurals"),
    FONT("font", "font");

    override fun toString(): String = typeName

    companion object {
        fun fromString(str: String): ResourceType? =
            ResourceType.values().firstOrNull { it.typeName.equals(str, true) }
    }
}

internal data class ResourceItem(
    val type: ResourceType,
    val qualifiers: List<String>,
    val name: String,
    val path: Path,
    val contentHash: Int,
    val offset: Long = -1,
    val size: Long = -1,
)

private fun ResourceType.getClassName(): ClassName = when (this) {
    ResourceType.DRAWABLE -> ClassName("org.jetbrains.compose.resources", "DrawableResource")
    ResourceType.FONT -> ClassName("org.jetbrains.compose.resources", "FontResource")
    ResourceType.STRING -> ClassName("org.jetbrains.compose.resources", "StringResource")
    ResourceType.STRING_ARRAY -> ClassName("org.jetbrains.compose.resources", "StringArrayResource")
    ResourceType.PLURAL_STRING -> ClassName("org.jetbrains.compose.resources", "PluralStringResource")
}

private fun ResourceType.requiresKeyName() =
    this in setOf(ResourceType.STRING, ResourceType.STRING_ARRAY, ResourceType.PLURAL_STRING)

private val resourceItemClass = ClassName("org.jetbrains.compose.resources", "ResourceItem")
private val internalAnnotationClass = ClassName("org.jetbrains.compose.resources", "InternalResourceApi")
private val internalAnnotation = AnnotationSpec.builder(internalAnnotationClass).build()

private val resourceContentHashAnnotationClass = ClassName("org.jetbrains.compose.resources", "ResourceContentHash")

private fun CodeBlock.Builder.addQualifiers(resourceItem: ResourceItem): CodeBlock.Builder {
    val languageQualifier = ClassName("org.jetbrains.compose.resources", "LanguageQualifier")
    val regionQualifier = ClassName("org.jetbrains.compose.resources", "RegionQualifier")
    val themeQualifier = ClassName("org.jetbrains.compose.resources", "ThemeQualifier")
    val densityQualifier = ClassName("org.jetbrains.compose.resources", "DensityQualifier")

    val languageRegex = Regex("[a-z]{2,3}")
    val regionRegex = Regex("r[A-Z]{2}")

    val qualifiersMap = mutableMapOf<ClassName, String>()

    fun saveQualifier(className: ClassName, qualifier: String) {
        qualifiersMap[className]?.let {
            error("${resourceItem.path} contains repetitive qualifiers: '$it' and '$qualifier'.")
        }
        qualifiersMap[className] = qualifier
    }

    resourceItem.qualifiers.forEach { q ->
        when (q) {
            "light",
            "dark" -> {
                saveQualifier(themeQualifier, q)
            }

            "mdpi",
            "hdpi",
            "xhdpi",
            "xxhdpi",
            "xxxhdpi",
            "ldpi" -> {
                saveQualifier(densityQualifier, q)
            }

            else -> when {
                q.matches(languageRegex) -> {
                    saveQualifier(languageQualifier, q)
                }

                q.matches(regionRegex) -> {
                    saveQualifier(regionQualifier, q)
                }

                else -> error("${resourceItem.path} contains unknown qualifier: '$q'.")
            }
        }
    }
    qualifiersMap[themeQualifier]?.let { q -> add("%T.${q.uppercase()}, ", themeQualifier) }
    qualifiersMap[densityQualifier]?.let { q -> add("%T.${q.uppercase()}, ", densityQualifier) }
    qualifiersMap[languageQualifier]?.let { q -> add("%T(\"$q\"), ", languageQualifier) }
    qualifiersMap[regionQualifier]?.let { q ->
        val lang = qualifiersMap[languageQualifier]
        if (lang == null) {
            error("Region qualifier must be used only with language.\nFile: ${resourceItem.path}")
        }
        val langAndRegion = "$lang-$q"
        if (!resourceItem.path.toString().contains("-$langAndRegion")) {
            error("Region qualifier must be declared after language: '$langAndRegion'.\nFile: ${resourceItem.path}")
        }
        add("%T(\"${q.takeLast(2)}\"), ", regionQualifier)
    }

    return this
}

internal fun getResFileSpec(
    packageName: String,
    className: String,
    moduleDir: String,
    isPublic: Boolean
): FileSpec {
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL
    return FileSpec.builder(packageName, className).also { file ->
        file.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("%T::class", internalAnnotationClass)
                .build()
        )
        file.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
                .addMember("%S", "RedundantVisibilityModifier")
                .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                .build()
        )
        file.addType(TypeSpec.objectBuilder(className).also { resObject ->
            resObject.addModifiers(resModifier)

            //readFileBytes
            val readResourceBytes = MemberName("org.jetbrains.compose.resources", "readResourceBytes")
            resObject.addFunction(
                FunSpec.builder("readBytes")
                    .addKdoc(
                        """
                    Reads the content of the resource file at the specified path and returns it as a byte array.
                    
                    Example: `val bytes = ${className}.readBytes("files/key.bin")`
                    
                    @param path The path of the file to read in the compose resource's directory.
                    @return The content of the file as a byte array.
                """.trimIndent()
                    )
                    .addParameter("path", String::class)
                    .addModifiers(KModifier.SUSPEND)
                    .returns(ByteArray::class)
                    .addStatement("""return %M("$moduleDir" + path)""", readResourceBytes)
                    .build()
            )

            //getUri
            val getResourceUri = MemberName("org.jetbrains.compose.resources", "getResourceUri")
            resObject.addFunction(
                FunSpec.builder("getUri")
                    .addKdoc(
                        """
                    Returns the URI string of the resource file at the specified path.
                    
                    Example: `val uri = ${className}.getUri("files/key.bin")`
                    
                    @param path The path of the file in the compose resource's directory.
                    @return The URI string of the file.
                """.trimIndent()
                    )
                    .addParameter("path", String::class)
                    .returns(String::class)
                    .addStatement("""return %M("$moduleDir" + path)""", getResourceUri)
                    .build()
            )

            ResourceType.values().forEach { type ->
                resObject.addType(TypeSpec.objectBuilder(type.accessorName).build())
            }
        }.build())
    }.build()
}

// We need to divide accessors by different files because
//
// if all accessors are generated in a single object
// then a build may fail with: org.jetbrains.org.objectweb.asm.MethodTooLargeException: Method too large: Res$drawable.<clinit> ()V
// e.g. https://github.com/JetBrains/compose-multiplatform/issues/4285
//
// if accessor initializers are extracted from the single object but located in the same file
// then a build may fail with: org.jetbrains.org.objectweb.asm.ClassTooLargeException: Class too large: Res$drawable
private val ITEMS_PER_FILE_LIMIT = 100
internal fun getAccessorsSpecs(
    //type -> id -> items
    resources: Map<ResourceType, Map<String, List<ResourceItem>>>,
    packageName: String,
    sourceSetName: String,
    moduleDir: String,
    resClassName: String,
    isPublic: Boolean,
    generateResourceContentHashAnnotation: Boolean
): List<FileSpec> {
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL
    val files = mutableListOf<FileSpec>()

    //we need to sort it to generate the same code on different platforms
    sortResources(resources).forEach { (type, idToResources) ->
        val chunks = idToResources.keys.chunked(ITEMS_PER_FILE_LIMIT)

        chunks.forEachIndexed { index, ids ->
            files.add(
                getChunkFileSpec(
                    type,
                    "${type.accessorName.uppercaseFirstChar()}$index.$sourceSetName",
                    sourceSetName.uppercaseFirstChar() + type.accessorName.uppercaseFirstChar() + index,
                    packageName,
                    moduleDir,
                    resClassName,
                    resModifier,
                    idToResources.subMap(ids.first(), true, ids.last(), true),
                    generateResourceContentHashAnnotation
                )
            )
        }
    }

    return files
}

private fun getChunkFileSpec(
    type: ResourceType,
    fileName: String,
    chunkClassName: String,
    packageName: String,
    moduleDir: String,
    resClassName: String,
    resModifier: KModifier,
    idToResources: Map<String, List<ResourceItem>>,
    generateResourceContentHashAnnotation: Boolean
): FileSpec {
    return FileSpec.builder(packageName, fileName).also { chunkFile ->
        chunkFile.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("%T::class", internalAnnotationClass)
                .build()
        )

        chunkFile.addProperty(
            PropertySpec.builder("MD", String::class)
                .addModifiers(KModifier.PRIVATE, KModifier.CONST)
                .initializer("%S", moduleDir)
                .build()
        )

        idToResources.forEach { (resName, items) ->
            val initializer = CodeBlock.builder()
                .beginControlFlow("lazy {")
                .apply {
                    if (type.requiresKeyName()) {
                        add("%T(%S, %S, setOf(\n", type.getClassName(), "$type:$resName", resName)
                    } else {
                        add("%T(%S, setOf(\n", type.getClassName(), "$type:$resName")
                    }
                    items.forEach { item ->
                        add("  %T(setOf(", resourceItemClass)
                        addQualifiers(item)
                        add("), ")
                        //file separator should be '/' on all platforms
                        add("\"${'$'}{MD}${item.path.invariantSeparatorsPathString}\", ${item.offset}, ${item.size}")
                        add("),\n")
                    }
                    add("))\n")
                }
                .endControlFlow()
                .build()

            val accessorBuilder = PropertySpec.builder(resName, type.getClassName(), resModifier)
                .receiver(ClassName(packageName, resClassName, type.accessorName))
                .delegate(initializer)
            if (generateResourceContentHashAnnotation) {
                accessorBuilder.addAnnotation(
                    AnnotationSpec.builder(resourceContentHashAnnotationClass)
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.DELEGATE)
                        .addMember("%L", items.fold(0) { acc, item -> ((acc * 31) + item.contentHash) })
                        .build()
                )
            }
            chunkFile.addProperty(accessorBuilder.build())
        }

        //__collect${chunkClassName}Resources function
        chunkFile.addFunction(
            FunSpec.builder("_collect${chunkClassName}Resources")
                .addAnnotation(internalAnnotation)
                .addModifiers(KModifier.INTERNAL)
                .addParameter(
                    "map",
                    MUTABLE_MAP.parameterizedBy(String::class.asClassName(), type.getClassName())
                )
                .also { collectFun ->
                    idToResources.keys.forEach { resName ->
                        collectFun.addStatement(
                            "map.put(%S, %N.%N.%N)",
                            resName,
                            resClassName,
                            type.accessorName,
                            resName
                        )
                    }
                }
                .build()
        )
    }.build()
}

internal fun getExpectResourceCollectorsFileSpec(
    packageName: String,
    fileName: String,
    resClassName: String,
    isPublic: Boolean
): FileSpec {
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL
    return FileSpec.builder(packageName, fileName).also { file ->
        ResourceType.values().forEach { type ->
            val typeClassName = type.getClassName()
            file.addProperty(
                PropertySpec
                    .builder(
                        "all${typeClassName.simpleName}s",
                        MAP.parameterizedBy(String::class.asClassName(), typeClassName),
                        KModifier.EXPECT,
                        resModifier
                    )
                    .receiver(ClassName(packageName, resClassName))
                    .build()
            )
        }
    }.build()
}

internal fun getActualResourceCollectorsFileSpec(
    packageName: String,
    fileName: String,
    resClassName: String,
    isPublic: Boolean,
    useActualModifier: Boolean, //e.g. java only project doesn't need actual modifiers
    typeToCollectorFunctions: Map<ResourceType, List<String>>
): FileSpec = FileSpec.builder(packageName, fileName).also { file ->
    val resModifier = if (isPublic) KModifier.PUBLIC else KModifier.INTERNAL

    file.addAnnotation(
        AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
            .addMember("org.jetbrains.compose.resources.InternalResourceApi::class")
            .build()
    )

    ResourceType.values().forEach { type ->
        val typeClassName = type.getClassName()
        val initBlock = CodeBlock.builder()
            .addStatement("lazy {").withIndent {
                addStatement("val map = mutableMapOf<String, %T>()", typeClassName)
                typeToCollectorFunctions.get(type).orEmpty().forEach { item ->
                    addStatement("%N(map)", item)
                }
                addStatement("return@lazy map")
            }
            .addStatement("}")
            .build()

        val mods = if (useActualModifier) {
            listOf(KModifier.ACTUAL, resModifier)
        } else {
            listOf(resModifier)
        }

        val property = PropertySpec
            .builder(
                "all${typeClassName.simpleName}s",
                MAP.parameterizedBy(String::class.asClassName(), typeClassName),
                mods
            )
            .receiver(ClassName(packageName, resClassName))
            .delegate(initBlock)
            .build()
        file.addProperty(property)
    }
}.build()

private fun sortResources(
    resources: Map<ResourceType, Map<String, List<ResourceItem>>>
): TreeMap<ResourceType, TreeMap<String, List<ResourceItem>>> {
    val result = TreeMap<ResourceType, TreeMap<String, List<ResourceItem>>>()
    resources
        .entries
        .forEach { (type, items) ->
            val typeResult = TreeMap<String, List<ResourceItem>>()
            items
                .entries
                .forEach { (name, resItems) ->
                    typeResult[name] = resItems.sortedBy { it.path }
                }
            result[type] = typeResult
        }
    return result
}
