package top.kagg886.epub.data

data class Spine(
    val toc: List<TOC>
) {
    val refs = toc.map { it.item }
}
