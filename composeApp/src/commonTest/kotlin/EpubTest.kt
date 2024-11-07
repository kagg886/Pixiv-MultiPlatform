import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import kotlin.test.Test

class EpubTest {
    @Test
    fun testEpub() {
        val book = Book()

        val client = OkHttpClient.Builder().build()
        with(book) {
            val body = client.newCall(Request.Builder().url("https://q.qlogo.cn/g?b=qq&nk=2513485574&s=160").build()).execute().body!!.byteStream().readBytes()
            coverImage = Resource(body, "cover.png")

            with(metadata) {
                addTitle("标题")
                addAuthor(Author("作者"))
                addDescription("简介")
            }

            val res = addResource(Resource(body,"1.png"))

            addSection("第一章",Resource("<img src=\"1.png\" alt=\"a\"></h1>".toByteArray(),"first.html"))
        }



        EpubWriter().write(book, File("test.epub").outputStream())
    }

}