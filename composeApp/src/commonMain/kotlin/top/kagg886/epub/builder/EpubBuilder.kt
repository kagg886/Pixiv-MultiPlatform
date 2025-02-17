package top.kagg886.epub.builder

import okio.Path
import top.kagg886.epub.Epub
import top.kagg886.epub.data.ResourceItem

class EpubBuilder(private val tempDir: Path) {

    companion object {
        operator fun invoke(tempDir: Path, block: EpubBuilder.() -> Unit): Epub {
            val builder = EpubBuilder(tempDir)
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
            temp = tempDir,
            metadata = metadata.build(),
            resources = manifest,
            spine = spine?.build()
        )
    }
}
