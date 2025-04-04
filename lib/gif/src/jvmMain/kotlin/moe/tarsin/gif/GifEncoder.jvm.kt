package moe.tarsin.gif

import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer
import okio.hashingSource
import okio.source
import top.kagg886.pmf.util.absolutePath
import top.kagg886.pmf.util.createNewFile
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.mkdirs
import top.kagg886.pmf.util.parentFile
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source

@OptIn(ExperimentalSerializationApi::class)
internal actual fun encodeGifPlatform(request: GifEncodeRequest) {
    val bytes = Cbor.encodeToByteArray(request)
    ByteBuffer.allocateDirect(bytes.size).apply {
        put(bytes)
        flip()
        encode(this)
    }
}

@Suppress("UnsafeDynamicallyLoadedCode")
internal actual fun loadNativeGifEncoder() {
    val name = when (jvmTarget) {
        JvmTarget.WINDOWS -> "gif_rust.dll"
        JvmTarget.LINUX -> "libgif_rust.so"
        JvmTarget.MACOS -> "libgif_rust.dylib"
    }

    val libPath = System.getProperty("user.home").toPath()
        .resolve(".config")
        .resolve("pmf")
        .resolve("lib")
        .resolve(name)

    if (!libPath.exists()) {
        val stream = GIFEncoderBuilderScope::class.java.getResourceAsStream("/$name")!!.source()
        exportLibToPath(stream, libPath)
    } else {
        val newHash = GIFEncoderBuilderScope::class.java.getResourceAsStream("/gif-build.hash")!!.readBytes().decodeToString()
        val oldHash = (libPath.source() as Source).use { it.hashingSource(MessageDigest.getInstance("MD5")).hash.hex().lowercase() }
        if (newHash != oldHash) {
            val stream = GIFEncoderBuilderScope::class.java.getResourceAsStream("/$name")!!.source()
            exportLibToPath(stream, libPath)
        }
    }
    System.load(libPath.absolutePath().toString())
}

private fun exportLibToPath(stream: Source, libPath: Path) {
    libPath.parentFile()!!.mkdirs()
    libPath.createNewFile()
    stream.use { i ->
        libPath.sink().buffer().use { o ->
            o.write(i.buffer().readByteArray())
            o.flush()
        }
    }
}
