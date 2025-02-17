package top.kagg886.epub.nodes.toc

import korlibs.io.serialization.xml.XmlBuilder
import korlibs.io.serialization.xml.buildXml
import top.kagg886.epub.RootScope

/**
 * <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1" xml:lang="en-US">
 *     <head>
 *     </head>
 *     <docTitle>
 *     </docTitle>
 *     <docAuthor>
 *     </docAuthor>
 *     <navMap>
 *     </navMap>
 * </ncx>
 */
inline fun RootScope.ncx(crossinline block: XmlBuilder.() -> Unit) = buildXml(
    rootTag = "ncx",
    props = arrayOf(
        "xmlns" to "http://www.daisy.org/z3986/2005/ncx/",
        "version" to "2005-1",
        "xml:lang" to "en-US"
    ),
    block = block
).toString()

inline fun XmlBuilder.head(crossinline block: XmlBuilder.() -> Unit) = node(
    tag = "head",
    block = block,
)

inline fun XmlBuilder.docTitle(text: String) = node(
    tag = "docTitle",
    block = {
        text(text)
    },
)

inline fun XmlBuilder.docAuthor(text: String) = node(
    tag = "docAuthor",
    block = {
        text(text)
    },
)

inline fun XmlBuilder.navMap(crossinline block: XmlBuilder.() -> Unit) = node(
    tag = "navMap",
    block = block,
)

inline fun XmlBuilder.navPoint(
    id: String,
    playOrder: String,
    label: String,
    content: String,
) = node(
    tag = "navPoint",
    props = arrayOf(
        "id" to id,
        "playOrder" to playOrder,
    ),
    block = {
        node("navLabel") {
            node("text") {
                text(label)
            }
        }

        node("content", props = arrayOf("src" to content))
    }
)
