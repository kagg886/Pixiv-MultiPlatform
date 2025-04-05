package moe.tarsin.gif

import java.nio.ByteBuffer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray

@OptIn(ExperimentalSerializationApi::class)
internal actual fun encodeGifPlatform(request: GifEncodeRequest) {
    val bytes = Cbor.encodeToByteArray(request)
    ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        flip()
        encode(this)
    }
}

actual fun loadNativeGifEncoder() = System.loadLibrary("gif_rust")
