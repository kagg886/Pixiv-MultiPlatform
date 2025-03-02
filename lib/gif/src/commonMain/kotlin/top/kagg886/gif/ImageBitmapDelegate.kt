package top.kagg886.gif

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import top.e404.skiko.gif.structure.Bitmap

class ImageBitmapDelegate(private val delegate:ImageBitmap) :Bitmap {
    private val pixelMap = delegate.toPixelMap()

    override fun computeIsOpaque(): Boolean {
        return !delegate.hasAlpha
    }

    override val width: Int = delegate.width
    override val height: Int = delegate.height

    override fun getColor(x: Int, y: Int): Int {
        return pixelMap[x, y].toArgb()
    }

    override fun getAlphaf(x: Int, y: Int): Float {
        return if (delegate.hasAlpha) {
            pixelMap[x, y].alpha
        } else {
            1F
        }
    }
}
