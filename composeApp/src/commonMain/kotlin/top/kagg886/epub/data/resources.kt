package top.kagg886.epub.data

import okio.Source
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ResourceItem(
    val file: Source,
    val extension: String,
    val mediaType: String
) {
    @OptIn(ExperimentalUuidApi::class)
    val uuid by lazy { Uuid.random().toHexString() }

    val fileName by lazy { "$uuid.$extension" }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceItem) return false

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }


}
