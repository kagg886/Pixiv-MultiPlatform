package top.e404.skiko.gif

import okio.BufferedSink

class ColorTable(
    val colors: IntArray,
    val sort: Boolean,
    val transparency: Int = (colors.capacity() - 1).coerceAtLeast(0),
    val background: Int = 0,
) {
    companion object {
        private val SizeList = listOf(0, 2, 4, 8, 16, 32, 64, 128, 256)
        private fun IntArray.capacity() = SizeList.firstOrNull { it >= size } ?: -1
        val Empty: ColorTable = ColorTable(IntArray(0), false)
    }

    init {
        check(colors.capacity() != -1) { "Color Table Too Large" }
    }

    fun s() = colors.size * 3 + (colors.capacity() - colors.size) * 3

    fun write(buffer: BufferedSink) {
        for (color in colors) {
            buffer.write(color.asRGBBytes().reversedArray())
        }
        buffer.write(ByteArray((colors.capacity() - colors.size) * 3).reversedArray())
    }

    fun exists(): Boolean = colors.isNotEmpty()

    fun size(): Int = when (colors.size) {
        in 129..256 -> 0x07
        in 65..128 -> 0x06
        in 33..64 -> 0x05
        in 17..32 -> 0x04
        in 9..16 -> 0x03
        in 5..8 -> 0x02
        in 3..4 -> 0x01
        in 0..2 -> 0x00
        else -> throw IllegalArgumentException("Color Table Size: ${colors.size}")
    }
}
