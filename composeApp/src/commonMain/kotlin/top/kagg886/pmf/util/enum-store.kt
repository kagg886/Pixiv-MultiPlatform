package top.kagg886.pmf.util

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T : Enum<T>> Settings.enum(key: String? = null, defaultValue: T): ReadWriteProperty<Any?, T> =
    EnumDelegate(this, key, defaultValue)


inline fun <reified T : Enum<T>> Settings.enumNullable(key: String? = null): ReadWriteProperty<Any?, T?> =
    enumNullable(key, T::class.java)

fun <T : Enum<T>> Settings.enumNullable(key: String? = null, enumClass: Class<T>): ReadWriteProperty<Any?, T?> =
    EnumNullableDelegate(this, key, enumClass)


private class EnumDelegate<T : Enum<T>>(
    private val settings: Settings,
    key: String?,
    private val defaultValue: T
) : OptionalKeyDelegate<T>(key) {
    override fun getValue(key: String): T {
        return kotlin.runCatching { java.lang.Enum.valueOf(defaultValue.javaClass, settings.get<String>(key)) }
            .getOrElse { defaultValue }
    }

    override fun setValue(key: String, value: T) {
        settings[key] = value.toString()
    }
}

private class EnumNullableDelegate<T : Enum<T>>(
    private val settings: Settings,
    key: String?,
    private val enumClass: Class<T>
) : OptionalKeyDelegate<T?>(key) {
    override fun getValue(key: String): T? {
        return kotlin.runCatching { java.lang.Enum.valueOf(enumClass, settings.get<String>(key)) }.getOrNull()
    }

    override fun setValue(key: String, value: T?) {
        settings[key] = value.toString()
    }
}


private abstract class OptionalKeyDelegate<T>(private val key: String?) : ReadWriteProperty<Any?, T> {

    abstract fun getValue(key: String): T
    abstract fun setValue(key: String, value: T)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getValue(key ?: property.name)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setValue(key ?: property.name, value)
    }
}