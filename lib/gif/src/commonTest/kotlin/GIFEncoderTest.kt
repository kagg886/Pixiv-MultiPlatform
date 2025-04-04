import moe.tarsin.gif.Frame
import moe.tarsin.gif.GifEncodeRequest
import moe.tarsin.gif.encodeGifPlatform
import kotlin.test.*

class NativeTest {

    @Test
    fun testNative() {
        val data = listOf(
            Frame("/home/tarsin/Pictures/屏幕截图/屏幕截图_20250327_121207.png", 100),
            Frame("/home/tarsin/Pictures/屏幕截图/屏幕截图_20250330_222527.png", 100),
        )
        val req = GifEncodeRequest(data, 15, "/home/tarsin/Pictures/test.gif")
        encodeGifPlatform(req)
    }
}
