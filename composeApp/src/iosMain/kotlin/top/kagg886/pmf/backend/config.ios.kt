package top.kagg886.pmf.backend

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.*

actual fun createConfigPlatform(file: Path):Settings {
    val settings = NSUserDefaults(
        suiteName = file.relativeTo(dataPath.resolve("config"))
            .segments.joinToString("-")
    )

    return NSUserDefaultsSettings(delegate = settings)
}


actual val dataPath: Path by lazy {
    val path = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        true
    )
    with(Path) {
        path[0]!!.toString().toPath()
    }
}
actual val cachePath: Path by lazy {
    NSTemporaryDirectory().toPath()
}