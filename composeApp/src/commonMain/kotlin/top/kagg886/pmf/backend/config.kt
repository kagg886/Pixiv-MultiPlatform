package top.kagg886.pmf.backend

import com.russhwolf.settings.Settings
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import okio.FileNotFoundException
import okio.Path
import okio.buffer
import okio.use
import top.kagg886.pmf.util.absolutePath
import top.kagg886.pmf.util.createNewFile
import top.kagg886.pmf.util.delete
import top.kagg886.pmf.util.deleteRecursively
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.util.mkdirs
import top.kagg886.pmf.util.parentFile
import top.kagg886.pmf.util.source
import top.kagg886.pmf.util.writeString

object SystemConfig {
    private val lock = reentrantLock()
    private val debounceJobScope = CoroutineScope(Dispatchers.IO)
    private val configCache = mutableMapOf<String, Settings>()

    fun getConfig(name: String = "default"): Settings {
        return configCache.getOrPut(name) {
            val file = dataPath.resolve("config").resolve("$name.properties", false)

            if (file.exists().not()) {
                file.absolutePath().parent?.mkdirs()
                file.createNewFile()
            }

            logger.d("load config from ${file.absolutePath()}")

            val flow = MutableSharedFlow<Map<String, JsonElement>>()

            debounceJobScope.launch {
                flow.debounce(1.seconds).collect {
                    file.writeString(lock.withLock { Json.encodeToString(it) })
                }
            }

            val settings = JsonDefaultSettings(
                onModify = { json ->
                    debounceJobScope.launch(Dispatchers.Unconfined) {
                        flow.emit(json)
                    }
                },
                delegate = runCatching {
                    Json.decodeFromString<JsonObject>(
                        file.source().buffer().use { it.readUtf8().ifEmpty { "{}" } },
                    )
                }.getOrElse {
                    logger.w("config create failed, now create a new config")
                    JsonObject(emptyMap())
                },
                lock = lock,
            )
            return settings
        }
    }
}

private class JsonDefaultSettings(
    delegate: JsonObject,
    val onModify: (Map<String, JsonElement>) -> Unit = {},
    val lock: ReentrantLock,
) : Settings {
    private val delegate = delegate.toMutableMap()
    override val keys = delegate.keys
    override val size = delegate.size

    override fun clear() {
        lock.withLock { delegate.clear() }
        onModify(delegate)
    }

    override fun getBoolean(key: String, defaultValue: Boolean) = lock.withLock { delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.boolean }

    override fun getBooleanOrNull(key: String) = lock.withLock { delegate[key]?.jsonPrimitive?.booleanOrNull }

    override fun getDouble(key: String, defaultValue: Double) = lock.withLock { delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.double }

    override fun getDoubleOrNull(key: String) = lock.withLock { delegate[key]?.jsonPrimitive?.doubleOrNull }

    override fun getFloat(key: String, defaultValue: Float) = lock.withLock { delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.float }

    override fun getFloatOrNull(key: String) = lock.withLock { delegate[key]?.jsonPrimitive?.floatOrNull }

    override fun getInt(key: String, defaultValue: Int) = lock.withLock { delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.int }

    override fun getIntOrNull(key: String) = lock.withLock { delegate[key]?.jsonPrimitive?.intOrNull }

    override fun getLong(key: String, defaultValue: Long) = lock.withLock { delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.long }

    override fun getLongOrNull(key: String) = lock.withLock { delegate[key]?.jsonPrimitive?.longOrNull }

    override fun getString(key: String, defaultValue: String) = lock.withLock { delegate.getOrElse(key) { JsonPrimitive(defaultValue) }.jsonPrimitive.content }

    override fun getStringOrNull(key: String) = lock.withLock { delegate[key]?.jsonPrimitive?.contentOrNull }

    override fun hasKey(key: String) = lock.withLock { delegate.containsKey(key) }

    override fun putBoolean(key: String, value: Boolean) {
        lock.withLock { delegate[key] = JsonPrimitive(value) }
        onModify(delegate)
    }

    override fun putDouble(key: String, value: Double) {
        lock.withLock { delegate[key] = JsonPrimitive(value) }
        onModify(delegate)
    }

    override fun putFloat(key: String, value: Float) {
        lock.withLock { delegate[key] = JsonPrimitive(value) }
        onModify(delegate)
    }

    override fun putInt(key: String, value: Int) {
        lock.withLock { delegate[key] = JsonPrimitive(value) }
        onModify(delegate)
    }

    override fun putLong(key: String, value: Long) {
        lock.withLock { delegate[key] = JsonPrimitive(value) }
        onModify(delegate)
    }

    override fun putString(key: String, value: String) {
        lock.withLock { delegate[key] = JsonPrimitive(value) }
        onModify(delegate)
    }

    override fun remove(key: String) {
        lock.withLock { delegate.remove(key) }
        onModify(delegate)
    }
}

expect val dataPath: Path
expect val cachePath: Path

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

inline fun <T : Any> useTempDir(block: (Path) -> T): T {
    val file = cachePath.resolve(Uuid.random().toHexString())
    file.mkdirs()

    try {
        return block(file)
    } finally {
        file.deleteRecursively()
    }
}
