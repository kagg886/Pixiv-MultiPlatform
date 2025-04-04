package moe.tarsin.gif

import java.nio.ByteBuffer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import top.kagg886.pmf.util.absolutePath
import top.kagg886.pmf.util.createNewFile
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.mkdirs
import top.kagg886.pmf.util.parentFile
import top.kagg886.pmf.util.sink

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
        libPath.parentFile()!!.mkdirs()
        libPath.createNewFile()
        stream.use { i->
            libPath.sink().buffer().use { o->
                o.write(i.buffer().readByteArray())
                o.flush()
            }
        }
    }
    System.load(libPath.absolutePath().toString())
}
