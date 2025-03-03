import korlibs.io.serialization.json.JsonFast
import korlibs.io.serialization.json.parseDyn
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import top.e404.skiko.gif.gif
import top.kagg886.gif.ImageBitmapDelegate
import top.kagg886.gif.toImageBitmap
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source
import kotlin.test.Test


class GIFCommonTest {
    @Test
    fun testGIFComposed() {
        val meta = JsonFast.parseDyn("test/meta.json".toPath().source().buffer().readUtf8()).value as ArrayList<LinkedHashMap<String,Any>>
        //  {
        //    "file": "000000.jpg",
        //    "delay": 200
        //  }
        val img = meta.map { m->
            "test/${m["file"]}".toPath().source().toImageBitmap() to m["delay"] as Int
        }

        val builder = gif(270,360) {
            table(ImageBitmapDelegate(img[0].first))
            loop(0)
            for (i in 1..118) {
                frame(ImageBitmapDelegate(img[i].first)) {
                    duration = img[i].second
                }
            }
        }
        "output.gif".toPath().sink().buffer().use {
            builder.buildToSink(it)
            it.flush()
        }
    }
}
