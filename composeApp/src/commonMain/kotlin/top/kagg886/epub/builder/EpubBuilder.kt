package top.kagg886.epub.builder

import top.kagg886.epub.Epub
import top.kagg886.epub.data.ResourceItem

class EpubBuilder {

    companion object {
        operator fun invoke(block: EpubBuilder.() -> Unit): Epub {
            val builder = EpubBuilder()
            builder.block()
            return builder.build()
        }
    }


    private var metadata = MetadataBuilder()
    private val manifest = mutableListOf<ResourceItem>()
    private var spine: SpineBuilder? = null


    fun metadata(block: MetadataBuilder.() -> Unit) {
        metadata = MetadataBuilder().apply(block)
    }


    fun manifest(block: MutableList<ResourceItem>.() -> Unit) {
        manifest.addAll(mutableListOf<ResourceItem>().apply(block))
    }


    fun spine(block: SpineBuilder.() -> Unit) {
        spine = SpineBuilder().apply(block)
    }

    fun build(): Epub {
        return Epub(
            metadata = metadata.build(),
            resources = manifest,
            spine = spine?.build()
        )
    }
}
