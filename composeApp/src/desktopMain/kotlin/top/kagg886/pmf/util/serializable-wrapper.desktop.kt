package top.kagg886.pmf.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

actual class SerializableWrapper<T : Any> {
    private var value: T? = null

    actual companion object {
        actual fun <T : Any> makeItSerializable(
            value: T,
            clazz: KClass<T>,
        ): SerializableWrapper<T> = SerializableWrapper<T>().apply {
            this.value = value
        }
    }

    actual operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value!!
}
