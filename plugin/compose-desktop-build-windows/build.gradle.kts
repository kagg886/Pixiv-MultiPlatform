plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "top.kagg886.compose.desktop.build"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("composePublishWindows") {
            id = "top.kagg886.compose.installer.windows"
            implementationClass = "top.kagg886.compose.installer.windows.PluginMain"
        }
    }
}

repositories {
    mavenCentral() // 添加 Maven Central 仓库
}

dependencies {
    implementation(kotlin("stdlib")) // 添加 Kotlin 标准库
    implementation(kotlin("reflect")) // 反射库
}

