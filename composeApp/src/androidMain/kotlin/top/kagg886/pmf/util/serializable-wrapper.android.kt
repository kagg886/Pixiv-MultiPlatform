package top.kagg886.pmf.util

import com.russhwolf.settings.get
import com.russhwolf.settings.set
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import top.kagg886.pmf.backend.SystemConfig

actual class PlatformSerializableWrapper<T : Any> : Externalizable, SerializableWrapper<T> {

    private var value: T? = null

    private var clazz: KClass<T>? = null

    @OptIn(InternalSerializationApi::class)
    override fun writeExternal(out: ObjectOutput) {
        out.writeUTF(clazz!!.qualifiedName!!)
        val json = Json.encodeToString(clazz!!.serializer(), value!!).toByteArray()

        out.writeInt(json.size)
        out.write(json)
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun readExternal(input: ObjectInput) {
        clazz = Class.forName(input.readUTF()).kotlin as KClass<T>

        val len = input.readInt()
        val json = ByteArray(len).apply { input.read(this, 0, len) }.decodeToString()
        value = Json.decodeFromString(clazz!!.serializer(), json)
    }

    override fun toString(): String = value.toString()

    actual override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value!!

    actual companion object {
        actual fun <T : Any> makeItSerializable(value: T, clazz: KClass<T>): SerializableWrapper<T> = PlatformSerializableWrapper<T>().apply {
            this.value = value
            this.clazz = clazz
        }
    }
}

actual class StorageSerializableWrapper<T : Any> actual constructor() : Externalizable, SerializableWrapper<T> {
    private var key: String? = null

    private var value: T? = null

    private var clazz: KClass<T>? = null

    @OptIn(InternalSerializationApi::class)
    override fun writeExternal(out: ObjectOutput) {
        out.writeUTF(key)
        out.writeUTF(clazz!!.qualifiedName!!)
        cache[key!!] = Json.encodeToString(clazz!!.serializer(), value!!).apply {
            logger.d("write $key: ${clazz!!.qualifiedName}($length)")
        }
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun readExternal(input: ObjectInput) {
        key = input.readUTF()
        clazz = Class.forName(input.readUTF()).kotlin as KClass<T>

        value = Json.decodeFromString(clazz!!.serializer(), cache[key!!]!!)

        logger.d("restore $key: ${clazz!!.qualifiedName}")
    }

    override fun toString(): String = value.toString()

    actual override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value!!

    actual companion object {
        internal val cache = SystemConfig.getConfig("cache")
        actual fun <T : Any> makeItSerializable(key: String, value: T, clazz: KClass<T>): SerializableWrapper<T> = StorageSerializableWrapper<T>().apply {
            this.key = key
            this.value = value
            this.clazz = clazz
        }
    }
}
