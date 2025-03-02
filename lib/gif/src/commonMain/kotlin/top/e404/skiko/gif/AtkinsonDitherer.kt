package top.e404.skiko.gif

import top.e404.skiko.gif.structure.Bitmap
import top.e404.skiko.util.rgb

/**
 * 抖动器
 */
internal object AtkinsonDitherer {
    private val DISTRIBUTION = listOf(
        ErrorComponent(1, 0, 1 / 8.0),
        ErrorComponent(2, 0, 1 / 8.0),

        ErrorComponent(-1, 1, 1 / 8.0),
        ErrorComponent(0, 1, 1 / 8.0),
        ErrorComponent(1, 1, 1 / 8.0),

        ErrorComponent(0, 2, 1 / 8.0),
    )

    private data class ErrorComponent(
        val deltaX: Int,
        val deltaY: Int,
        val power: Double,
    )

    private data class Color(val red: Int, val green: Int, val blue: Int) {
        constructor(rgb: Int) : this(
            red = rgb and 0xFF0000 shr 16,
            green = rgb and 0x00FF00 shr 8,
            blue = rgb and 0x0000FF
        )
    }

    private operator fun Color.minus(other: Color) = Color(
        red = this.red - other.red,
        green = this.green - other.green,
        blue = this.blue - other.blue
    )

    private operator fun Color.plus(other: Color) = Color(
        red = this.red + other.red,
        green = this.green + other.green,
        blue = this.blue + other.blue
    )

    private operator fun Color.times(power: Double) = Color(
        red = (this.red * power).toInt(),
        green = (this.green * power).toInt(),
        blue = (this.blue * power).toInt()
    )

    private fun Color.nearest() = red * red + green * green + blue * blue

    fun dither(bitmap: Bitmap, table: IntArray): IntArray {
        val width = bitmap.width
        val height = bitmap.height
        val colors = Array(height) { y -> Array(width) { x -> Color(rgb = bitmap.getColor(x, y)) } }
        val tableColors = List(table.size) { index -> Color(rgb = table[index]) }

        for (y in 0 until height) for (x in 0 until width) {
            val original = colors[y][x]
            val replacement = tableColors.minByOrNull { (it - original).nearest() }!!
            colors[y][x] = replacement
            val error = original - replacement
            for (component in DISTRIBUTION) {
                val siblingX = x + component.deltaX
                val siblingY = y + component.deltaY
                if (siblingX in 0 until width && siblingY in 0 until height) {
                    val offset = error * component.power
                    colors[siblingY][siblingX] = colors[siblingY][siblingX] + offset
                }
            }
        }

        val new = IntArray(bitmap.width * bitmap.height)

        for ((y, lines) in colors.withIndex()) for ((x, cell) in lines.withIndex()) {
            // XXX Alpha
            new[y * width + x] = if (bitmap.getAlphaf(x, y) < 0.5F) Int.MIN_VALUE
            else cell.run { rgb(red, green, blue) }
        }

        return new
    }
}
