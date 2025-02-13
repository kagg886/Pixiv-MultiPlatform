package top.kagg886.pmf.ui.util

import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

//值为byte
@JvmInline
value class Size(val bytes: Long) : Comparable<Size> {

    private fun Float.formatToString(count:Int):String {
        val str = this.toString()
        if (!str.contains(".")) {
            return this.roundToInt().toString()
        }
        return with(str.split(".")) {
            this[0] + "." + this[1].take(count)
        }
    }

    override fun toString(): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${kb.formatToString(2)} MB"
            else -> "${mb.formatToString(2)} MB"
        }
    }

    val kb: Float
        get() = bytes / 1024f
    val mb: Float
        get() = kb / 1024f

    override fun compareTo(other: Size): Int {
        return bytes.compareTo(other.bytes)
    }

    operator fun contains(other: LongRange): Boolean {
        return bytes in other
    }

    operator fun rangeTo(other: Size): ClosedRange<Long> = ClosedSizeRange(this.bytes, other.bytes)
}

val Number.mb: Size
    get() = Size(this.toLong() * 1024 * 1024)

val Number.kb: Size
    get() = Size(this.toLong() * 1024)

val Number.b: Size
    get() = Size(this.toLong())

class ClosedSizeRange(
    private val a: Long,
    private val b: Long
) : ClosedRange<Long> by LongRange(a, b)