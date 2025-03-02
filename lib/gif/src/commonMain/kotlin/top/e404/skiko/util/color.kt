package top.e404.skiko.util

import top.e404.skiko.gif.structure.Bitmap

internal fun rgb(r: Int, g: Int, b: Int) = (r shl 16) or (g shl 8) or b
internal fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

internal fun Int.red() = this and 0xff0000 shr 16
internal fun Int.green() = this and 0xff00 shr 8
internal fun Int.blue() = this and 0xff
internal fun Bitmap.forEach(block: (x: Int, y: Int) -> Boolean) {
    for (x in 0 until width) for (y in 0 until height) {
        if (block(x, y)) return
    }
}

internal fun Bitmap.forEachColor(block: (color: Int) -> Boolean) {
    return forEach { x, y ->
        block(getColor(x, y))
    }
}
