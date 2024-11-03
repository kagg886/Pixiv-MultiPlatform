package top.kagg886.pmf.ui.util

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


inline fun <reified T : Any> wrap(value: T): SerializableWrapper<T> {
    return SerializableWrapper<T>().apply {
        this.value = value
        this.clazz = T::class
    }
}

class SerializableWrapper<T : Any> : Externalizable {

    var value: T? = null

    var clazz: KClass<T>? = null

    @OptIn(InternalSerializationApi::class)
    override fun writeExternal(out: ObjectOutput) {
        out.writeUTF(clazz!!.qualifiedName!!)
        out.writeUTF(Json.encodeToString(clazz!!.serializer(), value!!))
    }

    @OptIn(InternalSerializationApi::class)
    override fun readExternal(input: ObjectInput) {
        clazz = Class.forName(input.readUTF()).kotlin as KClass<T>
        value = Json.decodeFromString(clazz!!.serializer(), input.readUTF())
    }

    override fun toString(): String {
        return value.toString()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value!!
    }
}