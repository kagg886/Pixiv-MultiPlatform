package top.kagg886.pmf.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

actual class PlatformSerializableWrapper<T : Any> : SerializableWrapper<T> {
    private var value: T? = null
    actual override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value!!
    actual companion object {
        actual fun <T : Any> makeItSerializable(value: T, clazz: KClass<T>): SerializableWrapper<T> = PlatformSerializableWrapper<T>().apply {
            this.value = value
        }
    }
}

actual class StorageSerializableWrapper<T : Any> actual constructor() :
    SerializableWrapper<T> {
    private var value: T? = null
    actual override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value!!

    actual companion object {
        actual fun <T : Any> makeItSerializable(key: String, value: T, clazz: KClass<T>): SerializableWrapper<T> = StorageSerializableWrapper<T>().apply {
            this.value = value
        }
    }
}
