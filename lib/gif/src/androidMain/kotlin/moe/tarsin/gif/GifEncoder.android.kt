package moe.tarsin.gif

import java.nio.ByteBuffer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import okio.Path

@OptIn(ExperimentalSerializationApi::class)
actual fun encodeGifPlatform(request: GifEncodeRequest) {
    val bytes = Cbor.encodeToByteArray(request)
    ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        flip()
        encode(this)
    }
}

actual fun loadNativeGifEncoder(resourceDir: Path, dataDir: Path, platform: Platform, debug: Boolean) = System.loadLibrary("gif_rust")
