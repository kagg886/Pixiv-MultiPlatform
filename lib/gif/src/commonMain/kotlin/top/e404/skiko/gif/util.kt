package top.e404.skiko.gif

import top.e404.skiko.util.blue
import top.e404.skiko.util.green
import top.e404.skiko.util.red

internal fun Int.rgb() = Triple(red(), green(), blue())
internal fun Int.asUnsignedShort(): Short {
    check(this in 0..0xFFFF)
    return toShort()
}

internal fun Int.asUnsignedByte(): Byte {
    check(this in 0..0xFF)
    return toByte()
}

internal fun Int.asRGBBytes() = byteArrayOf(red().toByte(), green().toByte(), blue().toByte())

fun gif(
    width: Int,
    height: Int,
    block: GIFBuilder.() -> Unit
) = GIFBuilder(width, height)
    .apply(block)


