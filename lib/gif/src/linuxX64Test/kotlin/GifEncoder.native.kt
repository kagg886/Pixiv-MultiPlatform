import kotlin.test.Test
import moe.tarsin.gif.GifEncodeRequest
import moe.tarsin.gif.encodeGifPlatform

class NativeTest {

    @Test
    fun testNative() {
        val req = GifEncodeRequest(emptyList(), 0, "")
        encodeGifPlatform(req)
    }
}
