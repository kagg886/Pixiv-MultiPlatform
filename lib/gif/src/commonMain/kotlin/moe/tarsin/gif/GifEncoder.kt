package moe.tarsin.gif

import kotlinx.serialization.Serializable
import okio.Path

@Serializable
data class Frame(
    val file: String,
    val delay: Int,
)

@Serializable
data class GifEncodeRequest(
    val metadata: List<Frame>,
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
