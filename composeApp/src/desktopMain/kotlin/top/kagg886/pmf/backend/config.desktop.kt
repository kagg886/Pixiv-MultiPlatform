package top.kagg886.pmf.backend

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import java.util.Properties

actual fun createConfigPlatform(file: Path): Settings {
    val target = file.toFile()
    return PropertiesSettings(
        delegate = Properties().apply {
            if (target.exists().not()) {
                target.absoluteFile.parentFile!!.mkdirs()
                target.createNewFile()
            }
            load(target.inputStream())
        },
        onModify = {
            target.outputStream().use { stream ->
                it.store(stream, "")
            }
        }
    )
}

actual val dataPath: Path by lazy {
    File(System.getProperty("user.home")).resolve(".config").resolve("pmf").toOkioPath()
}

actual val cachePath: Path by lazy {
    dataPath.resolve("cache")
}