import okio.Buffer
import okio.Path.Companion.toPath
import top.kagg886.epub.builder.EpubBuilder
import top.kagg886.epub.data.ResourceItem
import top.kagg886.pmf.backend.cachePath
import kotlin.test.Test

class EpubTest {
    private fun pageN(int: Int): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Document</title>
            </head>
            <body>
                <p>This is Page $int！！！</p>
            </body>
            </html>
        """.trimIndent()
    }

    @Test
    fun testEPUBExport() {
        val pages = (1..10).map {
            ResourceItem(
                file = Buffer().write(pageN(it).encodeToByteArray()),
                extension = "html",
                mediaType = "application/xhtml+xml"
            )
        }
        val epub = EpubBuilder(cachePath.resolve("work")) {
            metadata {
                title("Test")
                description("QWQ")
                creator("kagg886")
                language("zh-CN")
            }

            manifest {
                addAll(pages)
            }

            spine {
                for (i in pages.indices) {
                    toc("Page $i", pages[i])
                }
            }
        }
        epub.writeTo("test.epub".toPath())
    }
}
