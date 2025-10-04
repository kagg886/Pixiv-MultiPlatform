package top.kagg886.epub

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Entities
import okio.Buffer
import okio.Path.Companion.toPath
import top.kagg886.epub.builder.EpubBuilder
import top.kagg886.epub.data.ResourceItem
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking

class EpubTest {
    private fun pageN(int: Int): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml" lang="en">
            <head>
                <meta charset="UTF-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                <title>Document</title>
            </head>
            <body>
                <p>This is Page $int！！！</p>
            </body>
            </html>
        """.trimIndent()
    }

    @Test
    fun testEPUBExport() = runBlocking {
        val pages = (1..10).map {
            ResourceItem(
                file = Buffer().write(pageN(it).encodeToByteArray()),
                extension = "html",
                mediaType = "application/xhtml+xml"
            )
        }
        val epub = EpubBuilder("cache".toPath()) {
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


    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun testEPUBExportKsoup() {
        fun buildHTML(chapter: Int): String = Document.createShell("")
            .apply {
                selectFirst("html")?.attr("xmlns", "http://www.w3.org/1999/xhtml")

                with(body()) {
                    appendElement("h1").text(chapter.toString())
                    appendElement("p").text(Uuid.random().toHexString())
                }
            }
            .apply {
                outputSettings().syntax(Document.OutputSettings.Syntax.xml) // XML/XHTML 语法
                .escapeMode(Entities.EscapeMode.xhtml)      // 按 XHTML 实体转义
                .charset("UTF-8")                           // 输出编码
                .prettyPrint(true)                         // 美化排版（可选）
            }
            .html()
            .apply(::println)

        val pages = (1..10).map {
            ResourceItem(
                file = Buffer().write(buildHTML(it).encodeToByteArray()),
                extension = "html",
                mediaType = "application/xhtml+xml"
            )
        }
        val epub = EpubBuilder("cache".toPath()) {
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
        runBlocking {
            epub.writeTo("test.epub".toPath())
        }
    }
}
