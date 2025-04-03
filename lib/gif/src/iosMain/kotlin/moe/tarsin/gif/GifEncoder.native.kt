package moe.tarsin.gif

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import moe.tarsin.gif.cinterop.encode_animated_image_unsafe
import okio.Path

@OptIn(ExperimentalSerializationApi::class, ExperimentalForeignApi::class)
actual fun encodeGifPlatform(request: GifEncodeRequest) {
    val bytes = Cbor.encodeToByteArray(request)
    bytes.usePinned { pinned ->
        val addr = pinned.addressOf(0)
        encode_animated_image_unsafe(addr, bytes.size)
    }
}

// Nothing to do, already linked.
actual fun loadNativeGifEncoder(resourceDir: Path, dataDir: Path, platform: Platform, debug: Boolean) = Unit
