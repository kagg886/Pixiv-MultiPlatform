package top.kagg886.pmf.ui.util

//值为byte
@JvmInline
value class Size(val bytes: Long) : Comparable<Size> {

    override fun toString(): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.2f", kb)} MB"
            else -> "${String.format("%.2f", mb)} MB"
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