package top.kagg886.pmf.backend

import com.russhwolf.settings.Settings
import okio.Path

private val configCache = mutableMapOf<String, Settings>()

object SystemConfig {
    fun getConfig(name: String = "default"): Settings {
        return configCache.getOrPut(name) {
            val f = dataPath.resolve("config").resolve("$name.properties",false)

            createConfigPlatform(file = f)
        }
    }
}

expect fun createConfigPlatform(file:Path):Settings
expect val dataPath: Path
expect val cachePath: Path
