package top.kagg886.pmf.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

interface SerializableWrapper<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
}

@Deprecated("use platform() instead", ReplaceWith("platform()", imports = ["top.kagg886.pmf.util.platform"]))
inline fun <reified T : Any> wrap(value: T): SerializableWrapper<T> = PlatformSerializableWrapper.makeItSerializable(value, T::class)

inline fun <reified T : Any> platform(value: T): SerializableWrapper<T> = PlatformSerializableWrapper.makeItSerializable(value, T::class)

inline fun <reified T : Any> storage(key: String, value: T): SerializableWrapper<T> = StorageSerializableWrapper.makeItSerializable(key, value, T::class)

expect class PlatformSerializableWrapper<T : Any> {

    companion object {
        fun <T : Any> makeItSerializable(value: T, clazz: KClass<T>): SerializableWrapper<T>
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
}

expect class StorageSerializableWrapper<T : Any>() : SerializableWrapper<T> {
    companion object {
        fun <T : Any> makeItSerializable(key: String, value: T, clazz: KClass<T>): SerializableWrapper<T>
    }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T
}
