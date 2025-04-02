package moe.tarsin.gif

import kotlinx.serialization.Serializable
import top.kagg886.pixko.module.ugoira.UgoiraFrame

@Serializable
data class GifEncodeRequest(
    val metadata: List<UgoiraFrame>,
    val speed: Int,
    val dstPath: String,
)

expect fun encodeGifPlatform(request: GifEncodeRequest)
expect fun loadNativeGifEncoder()
