package top.kagg886.pmf.util

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/20 17:56
 * ================================================
 */
import co.touchlab.kermit.Severity
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVOptions
import top.kagg886.mkmb.defaultMMKV
import top.kagg886.mkmb.initialize
import top.kagg886.pixko.module.user.getCurrentUserSimpleProfile
import top.kagg886.pmf.backend.dataPath
import top.kagg886.pmf.backend.pixiv.PixivConfig

class DirectMMKVDelegate<T>(
    private val mmkv: MMKV,
    private val key: String,
    private val default: T,
    private val reader: MMKV.(String) -> T,
    private val writer: MMKV.(String, T) -> Unit,
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = if (mmkv.exists(key)) {
        mmkv.reader(key)
    } else {
        default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        mmkv.writer(key, value)
    }
}

fun initializeMMKV() = MMKV.initialize(dataPath.resolve("config").absolutePath().toString()) {
    logFunc = { level, tag, it ->
        logger.log(
            severity = when (level) {
                MMKVOptions.LogLevel.Debug -> Severity.Debug
                MMKVOptions.LogLevel.Info -> Severity.Info
                MMKVOptions.LogLevel.Warning -> Severity.Warn
                MMKVOptions.LogLevel.Error -> Severity.Error
                MMKVOptions.LogLevel.None -> Severity.Assert
            },
            tag = "MMKV $tag",
            message = it,
            throwable = null,
        )
    }
}

fun MMKV.string(key: String, default: String = "") = DirectMMKVDelegate(this, key, default, MMKV::getString, MMKV::set)

fun MMKV.int(key: String, default: Int = 0) = DirectMMKVDelegate(this, key, default, MMKV::getInt, MMKV::set)

fun MMKV.boolean(key: String, default: Boolean = false) = DirectMMKVDelegate(this, key, default, MMKV::getBoolean, MMKV::set)

fun MMKV.long(key: String, default: Long = 0L) = DirectMMKVDelegate(this, key, default, MMKV::getLong, MMKV::set)

fun MMKV.float(key: String, default: Float = 0f) = DirectMMKVDelegate(this, key, default, MMKV::getFloat, MMKV::set)

fun MMKV.double(key: String, default: Double = 0.0) = DirectMMKVDelegate(this, key, default, MMKV::getDouble, MMKV::set)

fun MMKV.bytes(key: String, default: ByteArray = byteArrayOf()) = DirectMMKVDelegate(this, key, default, MMKV::getByteArray, MMKV::set)

fun MMKV.stringSet(key: String, default: List<String> = listOf()) = DirectMMKVDelegate(this, key, default, MMKV::getStringList, MMKV::set)

fun <T : Any> MMKV.jsonOrNull(
    key: String,
    default: T? = null,
    json: Json = Json,
    module: KSerializer<T>,
): ReadWriteProperty<Any?, T?> = DirectMMKVDelegate(
    mmkv = this,
    key = key,
    default = default,
    reader = { k ->
        try {
            json.decodeFromString(module, getString(k))
        } catch (_: Exception) {
            default
        }
    },
    writer = { k, value ->
        if (value != null) {
            set(k, json.encodeToString(module, value))
        } else {
            remove(k)
        }
    },
)

inline fun <reified T : Any> MMKV.jsonOrNull(key: String, default: T? = null, json: Json = Json) = jsonOrNull(key, default, json, json.serializersModule.serializer<T>())

fun <T : Any> MMKV.json(
    key: String,
    default: T,
    json: Json = Json,
    module: KSerializer<T>,
): ReadWriteProperty<Any?, T> = DirectMMKVDelegate(
    mmkv = this,
    key = key,
    default = default,
    reader = { k ->
        try {
            json.decodeFromString(module, getString(k))
        } catch (_: Exception) {
            default
        }
    },
    writer = { k, value ->
        set(k, json.encodeToString(module, value))
    },
)

inline fun <reified T : Any> MMKV.json(key: String, default: T, json: Json = Json) = json(key, default, json, json.serializersModule.serializer())
