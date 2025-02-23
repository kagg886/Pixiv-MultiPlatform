package top.kagg886.epub.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Metadata(
    val title: String,
    val description: String? = null,
    val creator: String? = null,
    val publisher: String? = null,
    val rights: String? = null,
    val identifier: String = Uuid.random().toHexString(),
    val language: String,
    val meta: Map<String, String> = mapOf()
)
