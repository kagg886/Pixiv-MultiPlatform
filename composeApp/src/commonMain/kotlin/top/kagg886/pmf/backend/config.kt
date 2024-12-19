package top.kagg886.pmf.backend

import com.russhwolf.settings.Settings
import java.io.File
import java.util.*

private val configCache = mutableMapOf<String, Settings>()

object SystemConfig {
    fun getConfig(name: String = "default"): Settings {
        return configCache.getOrPut(name) {
            val f = File(rootPath.resolve("config"), "$name.properties")
            PropertiesSettings(
                delegate = Properties().apply {
                    if (!f.exists()) {
                        f.parentFile!!.mkdirs()
                        f.createNewFile()
                    }
                    f.inputStream().use {
                        load(it)
                    }
                },
                onModify = {
                    f.outputStream().use { stream ->
                        it.store(stream, "")
                    }
                }
            )
        }
    }
}

expect val rootPath: File


private class PropertiesSettings(
    private val delegate: Properties,
    private val onModify: (Properties) -> Unit = {}
) : Settings {

    @Suppress("UNCHECKED_CAST")
    override val keys: Set<String> get() = delegate.propertyNames().toList().toSet() as Set<String>
    override val size: Int get() = delegate.size

    override fun clear() {
        delegate.clear()
        onModify(delegate)
    }

    override fun remove(key: String) {
        delegate.remove(key)
        onModify(delegate)
    }

    override fun hasKey(key: String): Boolean = delegate[key] != null

    override fun putInt(key: String, value: Int) {
        delegate.setProperty(key, value.toString())
        onModify(delegate)
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        delegate.getProperty(key)?.toInt() ?: defaultValue

    override fun getIntOrNull(key: String): Int? =
        delegate.getProperty(key)?.toInt()

    override fun putLong(key: String, value: Long) {
        delegate.setProperty(key, value.toString())
        onModify(delegate)
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        delegate.getProperty(key)?.toLong() ?: defaultValue

    override fun getLongOrNull(key: String): Long? =
        delegate.getProperty(key)?.toLong()

    override fun putString(key: String, value: String) {
        delegate.setProperty(key, value)
        onModify(delegate)
    }

    override fun getString(key: String, defaultValue: String): String =
        delegate.getProperty(key) ?: defaultValue

    override fun getStringOrNull(key: String): String? =
        delegate.getProperty(key)

    override fun putFloat(key: String, value: Float) {
        delegate.setProperty(key, value.toString())
        onModify(delegate)
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        delegate.getProperty(key)?.toFloat() ?: defaultValue

    override fun getFloatOrNull(key: String): Float? =
        delegate.getProperty(key)?.toFloat()

    override fun putDouble(key: String, value: Double) {
        delegate.setProperty(key, value.toString())
        onModify(delegate)
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        delegate.getProperty(key)?.toDouble() ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? =
        delegate.getProperty(key)?.toDouble()

    override fun putBoolean(key: String, value: Boolean) {
        delegate.setProperty(key, value.toString())
        onModify(delegate)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        delegate.getProperty(key)?.toBoolean() ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? =
        delegate.getProperty(key)?.toBoolean()
}
