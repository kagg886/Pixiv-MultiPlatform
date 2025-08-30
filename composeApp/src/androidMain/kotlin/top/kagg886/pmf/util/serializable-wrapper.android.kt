package top.kagg886.pmf.util

import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

actual class SerializableWrapper<T : Any> : Externalizable {

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

    actual operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value!!

    actual companion object {
        actual fun <T : Any> makeItSerializable(value: T, clazz: KClass<T>): SerializableWrapper<T> = SerializableWrapper<T>().apply {
            this.value = value
            this.clazz = clazz
        }
    }
}
