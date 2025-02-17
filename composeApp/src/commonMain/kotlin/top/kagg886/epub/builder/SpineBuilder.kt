package top.kagg886.epub.builder

import top.kagg886.epub.data.ResourceItem
import top.kagg886.epub.data.Spine
import top.kagg886.epub.data.TOC

class SpineBuilder {
    private val toc = mutableListOf<TOC>()

    fun toc(title: String, resource: ResourceItem) {
        toc.add(TOC(title, resource))
    }

    fun build(): Spine {
        return Spine(toc)
    }
}
