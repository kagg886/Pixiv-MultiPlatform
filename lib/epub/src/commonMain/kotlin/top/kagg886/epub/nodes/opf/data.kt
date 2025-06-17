package top.kagg886.epub.nodes.opf

import korlibs.io.serialization.xml.XmlBuilder
import korlibs.io.serialization.xml.buildXml
import top.kagg886.epub.RootScope


/**
 * <?xml version="1.0" encoding="UTF-8"?>
 * <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
 *    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
 *    </metadata>
 *    <manifest>
 *    </manifest>
 *    <spine>
 *    </spine>
 * </package>
 */
internal inline fun RootScope.pkg(crossinline block: XmlBuilder.() -> Unit) = buildXml(
    rootTag = "package",
    props = arrayOf(
        "xmlns" to "http://www.idpf.org/2007/opf",
        "version" to "3.0",
        "unique-identifier" to "uid",
    ),
    block = block
).toString()

/**
 * <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
 * <dc:title id="title">Indexing for Editors and Authors: A Practical Guide to Understanding Indexes</dc:title>
 * <dc:description>为某人代笔的一篇作品，属于是全部xp都给加上了，真能看的爽吗？按设定来说，女主叫杨素洁，男主叫什么没说，真的方便带入吗？&lt;br/&gt;新人投稿，文笔不佳，希望喜欢，欢迎同好一起讨论。</dc:description>
 * <dc:creator>Fred Leise</dc:creator>
 * <dc:publisher>Information Today, Inc.</dc:publisher>
 * <dc:rights>Copyright &#x00A9; 2008 by American Society of Indexers, Inc.</dc:rights>
 * <dc:identifier id="p9781573878296">9781573878296</dc:identifier>
 * <dc:language>en-US</dc:language>
 * <meta name="cover" content="image_1" />
 * </metadata>
 */
internal fun XmlBuilder.description(text:String) = node(
    tag = "dc:description",
    block = { text(text) }
)
internal inline fun XmlBuilder.metadata(block: XmlBuilder.() -> Unit) = node(
    tag = "metadata",
    props = arrayOf(
        "xmlns:dc" to "http://purl.org/dc/elements/1.1/"
    ),
    block = block
)
internal fun XmlBuilder.dcMeta(name:String, value: String) = node(
    tag = "meta",
    props = arrayOf(
        "property" to name,
        "content" to value
    ),
)
internal fun XmlBuilder.dcDescription(text:String) = dcElement(tag = "description", value = text)
internal fun XmlBuilder.dcTitle(title: String) = dcElement(tag = "title", id = "title", value = title)
internal fun XmlBuilder.dcCreator(creator: String) = dcElement(tag = "creator", value = creator)
internal fun XmlBuilder.dcPublisher(publisher: String) = dcElement(tag = "publisher", value = publisher)
internal fun XmlBuilder.dcRights(rights: String) = dcElement(tag = "rights", value = rights)
internal fun XmlBuilder.dcIdentifier(identifier: String) = dcElement(tag = "identifier", id = "p$identifier", value = identifier)
internal fun XmlBuilder.dcLanguage(language: String) = dcElement(tag = "language", value = language)
internal fun XmlBuilder.dcElement(tag: String, id: String? = null, value: String) = node(
    tag = "dc:$tag",
    props = id?.let { arrayOf("id" to id) } ?: emptyArray(),
    block = { text(value) }
)


/**
 * <manifest>
 *     <item id="cover-image" properties="cover-image" href="images/9781573878296.jpg" media-type="image/jpeg" />
 *     <item id="style" href="css/stylesheet.css" media-type="text/css" />
 *     <item id="ncx" properties="nav" href="nav.xhtml" media-type="application/xhtml+xml" />
 *     <item id="ncx1" href="toc.ncx" media-type="application/x-dtbncx+xml" />
 *     <item id="copyright" href="copyright.xhtml" media-type="application/xhtml+xml" />
 *     <item id="Art_1.jpg" href="images/Art_1.jpg" media-type="image/jpeg" />
 *     <item id="titlepage" href="titlepage.xhtml" media-type="application/xhtml+xml" />
 * </manifest>
 */
internal inline fun XmlBuilder.manifest(block: XmlBuilder.() -> Unit) = node(
    tag = "manifest",
    block = block
)

internal fun XmlBuilder.item(id: String, properties: String? = null, href: String, mediaType: String) = node(
    tag = "item",
    props = buildList {
        add("id" to id)
        add("href" to href)
        properties?.let { add("properties" to it) }
        add("media-type" to mediaType)
    }.toTypedArray()
)

/**
 * <spine toc="ncx1">
 *         <itemref idref="cover" linear="yes" />
 *         <itemref idref="titlepage" linear="yes" />
 *         <itemref idref="copyright" linear="yes" />
 *         <itemref idref="toc" linear="yes" />
 *         <itemref linear="yes" idref="foreword001" />
 *         <itemref linear="yes" idref="introduction001" />
 *         <itemref linear="yes" idref="chapter001" />
 *         <itemref linear="yes" idref="chapter002" />
 *         <itemref linear="yes" idref="chapter003" />
 *         <itemref linear="yes" idref="chapter004" />
 *         <itemref linear="yes" idref="chapter005" />
 *         <itemref linear="yes" idref="chapter006" />
 *         <itemref linear="yes" idref="chapter007" />
 *         <itemref linear="yes" idref="chapter008" />
 *         <itemref linear="yes" idref="chapter009" />
 *         <itemref linear="yes" idref="chapter010" />
 *         <itemref linear="yes" idref="appendix001" />
 *         <itemref linear="yes" idref="appendix002" />
 *         <itemref linear="yes" idref="appendix003" />
 *         <itemref linear="yes" idref="author_bio001" />
 *         <itemref linear="yes" idref="index" />
 *         <itemref linear="yes" idref="ncx" />
 *     </spine>
 */
internal fun XmlBuilder.spine(toc: String? = null, block: XmlBuilder.() -> Unit) = node(
    tag = "spine",
    props = toc?.let { arrayOf("toc" to it) } ?: emptyArray(),
    block = block
)

internal fun XmlBuilder.itemRef(idref: String, linear: Boolean = true) = node(
    tag = "itemref",
    props = buildList {
        add("idref" to idref)
        if (!linear) add("linear" to "no")
    }.toTypedArray()
)
