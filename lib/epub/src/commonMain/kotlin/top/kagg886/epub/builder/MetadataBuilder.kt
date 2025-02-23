package top.kagg886.epub.builder

import top.kagg886.epub.data.Metadata
import kotlin.properties.Delegates
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MetadataBuilder {
    private var title: String by Delegates.notNull()
    private var description: String? = null
    private var creator: String? = null
    private var publisher: String? = null
    private var rights: String? = null
    @OptIn(ExperimentalUuidApi::class)
    private var identifier: String = Uuid.random().toHexString()
    private var language: String by Delegates.notNull()
    private val meta: MutableMap<String, String> = mutableMapOf()

    fun title(title: String) {
        this.title = title
    }

    fun description(description: String) {
        this.description = description
    }

    fun creator(creator: String) {
        this.creator = creator
    }

    fun publisher(publisher: String) {
        this.publisher = publisher
    }

    fun rights(rights: String) {
        this.rights = rights
    }

    fun identifier(identifier: String) {
        this.identifier = identifier
    }

    fun language(language: String) {
        this.language = language
    }

    fun meta(block: MutableMap<String, String>.()->Unit) {
        meta.putAll(mutableMapOf<String,String>().apply(block))
    }

    fun build(): Metadata {
        return Metadata(
            title = title,
            description = description,
            creator = creator,
            publisher = publisher,
            rights = rights,
            identifier = identifier,
            language = language,
            meta = meta
        )
    }
}
