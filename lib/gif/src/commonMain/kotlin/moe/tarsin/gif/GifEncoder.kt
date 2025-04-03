package moe.tarsin.gif

import kotlinx.serialization.Serializable
import okio.Path
import top.kagg886.pixko.module.ugoira.UgoiraFrame

@Serializable
data class GifEncodeRequest(
    val metadata: List<UgoiraFrame>,
    val speed: Int,
    val dstPath: String,
)

enum class Platform {
    Windows,
    Linux,
    MacOS,
    Other,
}

expect fun encodeGifPlatform(request: GifEncodeRequest)
expect fun loadNativeGifEncoder(resourceDir: Path, dataDir: Path, platform: Platform, debug: Boolean)
