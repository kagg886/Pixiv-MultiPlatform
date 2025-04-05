package moe.tarsin.gif

import kotlinx.serialization.Serializable
import okio.Path
import top.kagg886.pmf.util.absolutePath

@Serializable
internal data class Frame(
    val file: String,
    val delay: Int,
)

@Serializable
internal data class GifEncodeRequest(
    val metadata: List<Frame>,
    // 1-30, speed lower than slower.
    val speed: Int,
    val dstPath: String,
)

@DslMarker
annotation class GifEncoderDslMarker

class GIFEncoderBuilderScope internal constructor(private val output: Path) {
    private val metadata = mutableListOf<Frame>()
    private var speed = 15

    @GifEncoderDslMarker
    fun speed(speed: Int) {
        this.speed = speed
    }

    @GifEncoderDslMarker
    fun frame(path: Path, delay: Int = 1) {
        metadata += Frame(path.absolutePath().toString(), delay)
    }

    internal fun build() = GifEncodeRequest(metadata, speed, output.absolutePath().toString())
}

private val init by lazy { loadNativeGifEncoder() }

fun encodeGif(output: Path, block: GIFEncoderBuilderScope.() -> Unit) = with(init) {
    val request = GIFEncoderBuilderScope(output).apply(block)
    encodeGifPlatform(request.build())
}

internal expect fun encodeGifPlatform(request: GifEncodeRequest)
internal expect fun loadNativeGifEncoder()
