package top.kagg886.pmf.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty


inline fun <reified T : Any> wrap(value: T): SerializableWrapper<T> = SerializableWrapper.makeItSerializable(value, T::class)

expect class SerializableWrapper<T : Any> {

    companion object {
        fun <T : Any> makeItSerializable(value: T, clazz: KClass<T>): SerializableWrapper<T>
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
}