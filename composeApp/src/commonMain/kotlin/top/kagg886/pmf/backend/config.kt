package top.kagg886.pmf.backend

import com.russhwolf.settings.Settings
import io.ktor.utils.io.core.*
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okio.*
import okio.use
import top.kagg886.pmf.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val configCache = mutableMapOf<String, Settings>()

object SystemConfig {
    fun getConfig(name: String = "default"): Settings {
        return configCache.getOrPut(name) {
            val file = dataPath.resolve("config").resolve("$name.properties", false)

            if (file.exists().not()) {
                file.absolutePath().parent?.mkdirs()
                file.createNewFile()
            }

            logger.d("load config from ${file.absolutePath()}")

            val lock = reentrantLock()

            val settings = JsonDefaultSettings(
                delegate =
                    kotlin.runCatching {
                        Json.decodeFromString<JsonObject>(
                            file.source().buffer().use { it.readUtf8().ifEmpty { "{}" } }
                        )
                    }.getOrElse {
                        logger.w("config create failed, now create a new config")
                        JsonObject(emptyMap())
                    },
                onModify = { json ->
                    lock.withLock {
                        file.writeBytes(Json.encodeToString(json).toByteArray())
                    }
                }
            )
            return settings
        }
    }
}

private class JsonDefaultSettings(
    delegate: JsonObject,
    val onModify: (Map<String, JsonElement>) -> Unit = {}
) : Settings {
    private val delegate = delegate.toMutableMap()

    override val keys: Set<String> = this.delegate.keys
    override val size: Int = this.delegate.size

    override fun clear() {
        delegate.clear()
        onModify(delegate)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.boolean
    }

    override fun getBooleanOrNull(key: String): Boolean? {
        return delegate[key]?.jsonPrimitive?.booleanOrNull
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.double
    }

    override fun getDoubleOrNull(key: String): Double? {
        return delegate[key]?.jsonPrimitive?.doubleOrNull
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.float
    }

    override fun getFloatOrNull(key: String): Float? {
        return delegate[key]?.jsonPrimitive?.floatOrNull
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.int
    }

    override fun getIntOrNull(key: String): Int? {
        return delegate[key]?.jsonPrimitive?.intOrNull
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.long
    }

    override fun getLongOrNull(key: String): Long? {
        return delegate[key]?.jsonPrimitive?.longOrNull
    }

    override fun getString(key: String, defaultValue: String): String {
        return delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.content
    }

    override fun getStringOrNull(key: String): String? {
        return delegate[key]?.jsonPrimitive?.contentOrNull
    }

    override fun hasKey(key: String): Boolean {
        return delegate.containsKey(key)
    }

    override fun putBoolean(key: String, value: Boolean) {
        delegate[key] = JsonPrimitive(value)
        onModify(delegate)
    }

    override fun putDouble(key: String, value: Double) {
        delegate[key] = JsonPrimitive(value)
        onModify(delegate)
    }

    override fun putFloat(key: String, value: Float) {
        delegate[key] = JsonPrimitive(value)
        onModify(delegate)
    }

    override fun putInt(key: String, value: Int) {
        delegate[key] = JsonPrimitive(value)
        onModify(delegate)
    }

    override fun putLong(key: String, value: Long) {
        delegate[key] = JsonPrimitive(value)
        onModify(delegate)
    }

    override fun putString(key: String, value: String) {
        delegate[key] = JsonPrimitive(value)
        onModify(delegate)
    }

    override fun remove(key: String) {
        delegate.remove(key)
        onModify(delegate)
    }
}


expect val dataPath: Path
expect val cachePath: Path


@OptIn(ExperimentalUuidApi::class)
inline fun <T : Any> useTempFile(block: (Path) -> T): T {
    val file = cachePath.resolve(Uuid.random().toHexString())
    file.parentFile()?.mkdirs() ?: throw FileNotFoundException("parent file not found")
    file.createNewFile()

    try {
        return block(file)
    } finally {
        file.delete()
    }
}

@OptIn(ExperimentalUuidApi::class)
inline fun <T : Any> useTempDir(block: (Path) -> T): T {
    val file = cachePath.resolve(Uuid.random().toHexString())
    file.mkdirs()

    try {
        return block(file)
    } finally {
        file.deleteRecursively()
    }
}
