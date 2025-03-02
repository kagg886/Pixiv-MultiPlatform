import okio.buffer
import okio.sink
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skiko.toImage
import org.junit.Test
import top.e404.skiko.gif.gif
import java.io.File
import javax.imageio.ImageIO

class GIFTest {
    private class DelegatedBitmap(val bitmap: Bitmap) : top.e404.skiko.gif.structure.Bitmap {
        override fun computeIsOpaque(): Boolean {
            return bitmap.computeIsOpaque()
        }

        override val width: Int = bitmap.width
        override val height: Int = bitmap.height

        override fun getColor(x: Int, y: Int): Int = bitmap.getColor(x, y)
        override fun getAlphaf(x: Int, y: Int): Float {
            return bitmap.getAlphaf(x, y)
        }

    }
    @Test
    fun testGIF() {
        fun img(i:Int) = ImageIO.read(File("test/${i.toString().padStart(6,'0')}.jpg")).toImage()


        val builder = gif(270,360) {
            table(DelegatedBitmap(Bitmap.makeFromImage(img(0))))
            options {
                loop(0)
                duration = 150
            }
            for (i in 1..118) {
                frame(DelegatedBitmap(Bitmap.makeFromImage(img(i))))
            }

        }

        File("output.gif").sink().buffer().use {
            builder.buildToSink(it)
        }
    }
}
