package top.kagg886.filepicker.internal

import java.nio.charset.StandardCharsets
import okio.BufferedSource
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer
import okio.source
import top.kagg886.pmf.util.absolutePath
import top.kagg886.pmf.util.createNewFile
import top.kagg886.pmf.util.exists
import top.kagg886.pmf.util.mkdirs
import top.kagg886.pmf.util.parentFile
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/17 14:03
 * ================================================
 */

internal val jvmTarget by lazy {
    val osName = System.getProperty("os.name")
    when {
        osName.startsWith("Mac") -> JvmTarget.MACOS
        osName.startsWith("Win") -> JvmTarget.WINDOWS
        osName.startsWith("Linux") -> JvmTarget.LINUX
        else -> error("Unsupported OS: $osName")
    }
}

enum class JvmTarget {
    MACOS,
    WINDOWS,
    LINUX,
}


internal inline fun <R> useRes(name: String, f: BufferedSource.() -> R) = NativeFilePicker::class.java.getResourceAsStream(name)!!.source().buffer().use(f)

@Suppress("UnsafeDynamicallyLoadedCode")
internal fun initNativeLib() {
    val name = when (jvmTarget) {
        JvmTarget.WINDOWS -> "filepicker.dll"
        JvmTarget.LINUX -> "libfilepicker.so"
        JvmTarget.MACOS -> "libfilepicker.dylib"
    }

    val home = System.getProperty("user.home").toPath()
    val libPath = home / ".config" / "pmf" / "lib" / name
    if (!libPath.exists()) {
        useRes("/$name") { exportLibToPath(libPath) }
    } else {
        val newHash = useRes("/filepicker-build.hash") { readString(StandardCharsets.UTF_8) }
        val oldHash = libPath.md5()
        if (newHash != oldHash) useRes("/$name") { exportLibToPath(libPath) }
    }
    System.load(libPath.absolutePath().toString())
}

private fun BufferedSource.exportLibToPath(libPath: Path) {
    libPath.parentFile()!!.mkdirs()
    libPath.createNewFile()
    libPath.sink().use(::readAll)
}

fun Path.md5() = source().buffer().use { src ->
    HashingSink.md5(blackholeSink()).use { dst ->
        src.readAll(dst)
        dst.hash.hex().lowercase()
    }
}
