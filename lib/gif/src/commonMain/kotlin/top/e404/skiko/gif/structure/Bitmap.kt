package top.e404.skiko.gif.structure

interface Bitmap {
    /**
     * 所有元素不透明时返回true
     */
    fun computeIsOpaque(): Boolean

    val width: Int
    val height: Int

    /**
     * 返回像素点的颜色
     *
     * 在现有的版本中，可以忽略alpha值
     */
    fun getColor(x: Int, y: Int): Int

    fun getAlphaf(x: Int, y: Int): Float
}
