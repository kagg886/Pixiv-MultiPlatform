# Compose Desktop Windows Installer Plugin



> ## 致谢：
>
> 本项目在具体实现上照搬了[tangshimin/wix-package](https://github.com/tangshimin/wix-package)，该插件仅为其gradle构建脚本的封装。



## 1. 使用

```kotlin
id("top.kagg886.compose.installer.windows")
```



## 2.配置

```kotlin
configureComposeWindowsInstaller {
    appName = rootProject.name //应用名称(必须是英文的)
    appVersion = pkgVersion //版本号(a.b.c)字段
    shortcutName = rootProject.name //快捷方式名，只有这里的内容是给用户看的
    iconFile = project.file("icons/pixiv.ico") // 图标路径
    manufacturer = "kagg886" //安装包内显示的作者信息
}
```



## 3. 运行

```powershell
.\gradlew.bat :composeApp:light
```



## 4. 提取

安装包会生成在与binary同级的目录下，为`appName.msi`