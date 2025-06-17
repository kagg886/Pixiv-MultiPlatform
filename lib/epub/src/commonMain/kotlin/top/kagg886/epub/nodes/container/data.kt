package top.kagg886.epub.nodes.container

import korlibs.io.serialization.xml.XmlBuilder
import korlibs.io.serialization.xml.buildXml
import top.kagg886.epub.RootScope


/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container" version="1.0">
 *    <rootfiles>
 *       <rootfile full-path="EPUB/package.opf" media-type="application/oebps-package+xml"/>
 *    </rootfiles>
 * </container>
 */

internal inline fun RootScope.container(crossinline block: XmlBuilder.() -> Unit) = buildXml(
    rootTag = "container",
    props = arrayOf(
        "xmlns" to "urn:oasis:names:tc:opendocument:xmlns:container",
        "version" to "1.0",
    ),
    block = block
).toString()

internal inline fun XmlBuilder.rootFiles(crossinline block: XmlBuilder.() -> Unit) = node(
    tag = "rootfiles",
    block = block
)

internal fun XmlBuilder.rootFile(fullPath: String) = node(
    tag = "rootfile",
    props = arrayOf(
        "full-path" to fullPath,
        "media-type" to "application/oebps-package+xml"
    )
)
