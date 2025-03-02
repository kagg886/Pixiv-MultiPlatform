package top.e404.skiko.gif.structure

interface IRect {
    val left: Int
    val top: Int
    val width: Int
    val height: Int

    companion object {
        fun makeXYWH(i: Int, i1: Int, i2: Int, i3: Int): IRect {
            return object : IRect {
                override val left: Int = i
                override val top: Int = i1
                override val width: Int = i2
                override val height: Int = i3

            }
        }
    }
}
