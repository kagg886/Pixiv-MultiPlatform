package top.kagg886.compose.installer.windows.config

import java.io.File
import kotlin.properties.Delegates

@DslMarker
annotation class BuildExtensionMarker

@BuildExtensionMarker
open class BuildExtension {
    //快捷方式名
    var appName: String by Delegates.notNull()

    //版本号
    var appVersion: String by Delegates.notNull()

    //快捷方式名
    var shortcutName: String by Delegates.notNull()

    //图标路径
    var iconFile: File by Delegates.notNull()

    //发布者
    var manufacturer: String by Delegates.notNull()
}
