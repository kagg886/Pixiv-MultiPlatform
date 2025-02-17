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

inline fun RootScope.container(crossinline block: XmlBuilder.() -> Unit) = buildXml(
    rootTag = "container",
    props = arrayOf(
        "xmlns" to "urn:oasis:names:tc:opendocument:xmlns:container",
        "version" to "1.0",
    ),
    block = block
).toString()

inline fun XmlBuilder.rootFiles(crossinline block: XmlBuilder.() -> Unit) = node(
    tag = "rootfiles",
    block = block
)

inline fun XmlBuilder.rootFile(fullPath: String) = node(
    tag = "rootfile",
    props = arrayOf(
        "full-path" to fullPath,
        "media-type" to "application/oebps-package+xml"
    )
)


//@Serializable
//@SerialName("container")
//data class Container(
//    @XmlAttribute
//    val xmlns: String = "urn:oasis:names:tc:opendocument:xmlns:container",
//    @XmlAttribute
//    val version: String = "1.0",
//    val rootFile: RootFiles,
//)
//
//@Serializable
//@SerialName("rootfiles")
//data class RootFiles(
//    val rootFile: RootFile
//)
//
//@Serializable
//@SerialName("rootfile")
//data class RootFile(
//    @XmlAttribute
//    @SerialName("full-path")
//    val fullPath: String,
//    @XmlAttribute
//    @SerialName("media-type")
//    val mediaType: String = "application/oebps-package+xml"
//)
