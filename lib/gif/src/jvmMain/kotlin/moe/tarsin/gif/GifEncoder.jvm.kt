package moe.tarsin.gif

import java.io.File
import java.nio.ByteBuffer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip

@OptIn(ExperimentalSerializationApi::class)
actual fun encodeGifPlatform(request: GifEncodeRequest) {
    val bytes = Cbor.encodeToByteArray(request)
    ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        flip()
        encode(this)
    }
}

@Suppress("UnsafeDynamicallyLoadedCode")
actual fun loadNativeGifEncoder(resourceDir: Path, dataDir: Path, platform: Platform, debug: Boolean) {
    val name = when (platform) {
        Platform.Windows -> "gif_rust.dll"
        Platform.Linux -> "libgif_rust.so"
        Platform.MacOS -> "libgif_rust.dylib"
        Platform.Other -> throw IllegalArgumentException()
    }
    if (debug) {
        // Hack for debug build workdir
        System.load(File("lib/gif/src/rust/target/release/$name").absolutePath.replace("/composeApp", ""))
    } else {
        val jar = FileSystem.SYSTEM.list(resourceDir).find { e -> e.name.startsWith("gif-jvm") }
        requireNotNull(jar) { "Can't find library jar!" }
        val fs = FileSystem.SYSTEM.openZip(jar)
        val dst = dataDir / name
        fs.source(name.toPath()).buffer().use { src ->
            FileSystem.SYSTEM.delete(dst)
            FileSystem.SYSTEM.sink(dst).use { dst ->
                src.readAll(dst)
            }
        }
        System.load("$dst")
    }
}
