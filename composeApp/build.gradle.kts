import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val pkgName: String = "top.kagg886.pmf"

//val pkgVersion: String = "1.0.0"
//val pkgCode: Int = 1

val appVersionName = System.getenv("APP_VERSION_NAME") ?: project.findProperty("APP_VERSION_NAME") as? String ?: ""
check(appVersionName.startsWith("v")) {
    "APP_VERSION not supported, current is $appVersionName"
}
val pkgVersion: String = appVersionName.substring(1)
val pkgCode: Int = with(pkgVersion.split(".")) {
    val x = this[0].toInt()
    val y = this[1].toInt()
    val z = this[2].toInt()
    x + y + z + x * y + y * z + z * x
}

println("APP_VERSION: $pkgVersion($pkgCode)")


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.room)
    alias(libs.plugins.aboutlibrariesPlugin)
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
}
kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            //kotlin stdlib
            implementation(libs.kotlin.reflect)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            //compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            //voyager
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.bottom.sheet.navigator)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.transitions)
            implementation(libs.koin.core)

            //orbit
            implementation(libs.orbit.core)

            //pixiv
            implementation(libs.pixko)

            //storage
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.serialization)

            //settings-ui
            implementation(libs.compose.settings.ui)
            implementation(libs.compose.settings.extended)

            //webview
            api(libs.compose.webview.multiplatform)

            //image-loader
            implementation(libs.sketch.compose)
            implementation(libs.sketch.extensions.compose)
            implementation(libs.sketch.http.okhttp)
            implementation(libs.zoomimage.compose.sketch)

            //room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            //save file to storage
            implementation(libs.filekit.compose)

            //epub export
            implementation(libs.epublib.core.get().toString()) {
                exclude("xmlpull", "xmlpull")
                exclude("net.sf.kxml","kxml2")
            }
            implementation(libs.jsoup)

            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kxml2)
        }
    }
}

aboutLibraries {
    duplicationMode = MERGE
    duplicationRule = GROUP
}

android {
    namespace = pkgName
    compileSdk = 34

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

    defaultConfig {
        applicationId = pkgName
        minSdk = 24
        targetSdk = 34
        versionCode = pkgCode
        versionName = pkgVersion

        manifestPlaceholders["APP_NAME"] = rootProject.name

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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
        mainClass = "${pkgName}.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.AppImage)
            packageName = rootProject.name
            packageVersion = pkgVersion
        }
        buildTypes.release.proguard {
            isEnabled = false
        }

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}