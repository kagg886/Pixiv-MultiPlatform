package top.e404.skiko.gif.structure

interface Bitmap {
    fun computeIsOpaque(): Boolean

    val width: Int
    val height: Int

    fun getColor(x: Int, y: Int): Int

    fun getAlphaf(x: Int, y: Int): Float
}
