package moe.tarsin.gif

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import okio.BufferedSource
import okio.Path
import okio.Path.Companion.toPath
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

internal inline fun <R> useRes(name: String, f: BufferedSource.() -> R) = GIFEncoderBuilderScope::class.java.getResourceAsStream(name)!!.source().buffer().use(f)

@Suppress("UnsafeDynamicallyLoadedCode")
internal actual fun loadNativeGifEncoder() {
    val name = when (jvmTarget) {
        JvmTarget.WINDOWS -> "gif_rust.dll"
        JvmTarget.LINUX -> "libgif_rust.so"
        JvmTarget.MACOS -> "libgif_rust.dylib"
    }

    val home = System.getProperty("user.home").toPath()
    val libPath = home / ".config" / "pmf" / "lib" / name
    if (!libPath.exists()) {
        useRes("/$name") { exportLibToPath(libPath) }
    } else {
        val newHash = useRes("/gif-build.hash") { readString(StandardCharsets.UTF_8) }
        val oldHash = libPath.source().use { it.hashingSource(MessageDigest.getInstance("MD5")).hash.hex().lowercase() }
        if (newHash != oldHash) useRes("/$name") { exportLibToPath(libPath) }
    }
    System.load(libPath.absolutePath().toString())
}

private fun BufferedSource.exportLibToPath(libPath: Path) {
    libPath.parentFile()!!.mkdirs()
    libPath.createNewFile()
    libPath.sink().use(::readAll)
}
