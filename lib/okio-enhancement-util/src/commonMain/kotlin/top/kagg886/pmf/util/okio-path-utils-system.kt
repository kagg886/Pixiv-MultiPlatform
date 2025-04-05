package top.kagg886.pmf.util

import korlibs.io.file.std.createZipFromTreeTo
import korlibs.io.file.std.rootLocalVfs
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.Sink
import okio.Source
import okio.buffer
import okio.openZip
import okio.use

inline fun Path.meta() = FileSystem.SYSTEM.metadata(this)


fun Path.sink(append: Boolean = false): Sink = sink0(this, append)

internal fun sink0(path: Path, append: Boolean = false) = with(FileSystem.SYSTEM.openReadWrite(path)) {
    FileHandleSink(this, if (append) appendingSink() else sink(fileOffset = 0))
}

fun Source.transfer(sink: Sink) {
    val buf = Buffer()
    var len: Long
    while (this.read(buf, 1024).also { len = it } != -1L) {
        sink.write(buf, len)
        buf.clear()
    }
    sink.flush()
}

fun Path.writeBytes(byteArray: ByteArray) = FileSystem.SYSTEM.openReadWrite(this).apply {
    resize(0)
    sink().buffer().use {
        it.write(byteArray)
        it.flush()
    }
}.close()

inline fun Path.writeString(s: String) = writeBytes(s.encodeToByteArray())

inline fun Path.source() = FileSystem.SYSTEM.source(this)

inline fun Path.absolutePath() = this

inline fun Path.parentFile() = absolutePath().parent

inline fun Path.listFile() = FileSystem.SYSTEM.list(this)

inline fun Path.exists() = FileSystem.SYSTEM.exists(this)

inline fun Path.isDirectory() = meta().isDirectory

inline fun Path.mkdirs() = FileSystem.SYSTEM.createDirectories(this)

inline fun Path.mkdir() = FileSystem.SYSTEM.createDirectory(this)

inline fun Path.delete() = FileSystem.SYSTEM.delete(this)

inline fun Path.createNewFile() = sink().close()

inline fun Path.deleteRecursively() {
    FileSystem.SYSTEM.deleteRecursively(this)
    FileSystem.SYSTEM.delete(this)
}

fun Path.zip(target: Path = FileSystem.SYSTEM.canonicalize(this).parent!!.resolve("${this.name}.zip")): Path {
    val vfs = rootLocalVfs[absolutePath().toString()]
    target.parentFile()?.mkdirs()
    target.createNewFile()
    return runBlocking {
        vfs.createZipFromTreeTo(
            zipFile = rootLocalVfs[target.absolutePath().toString()]
        )
        target
    }
}

val Path.nameWithoutExtension
    get() = if (name.lastIndexOf(".") == -1) name else name.substring(0, name.lastIndexOf("."))

fun Path.unzip(target: Path = FileSystem.SYSTEM.canonicalize(this).parent!!.resolve(nameWithoutExtension)): Path {
    FileSystem.SYSTEM.openZip(this).use { sys->
        sys.listRecursively("/".toPath()).filterNot { sys.metadata(it).isDirectory }.forEach { entry->
            val tr = target.resolve(entry.relativeTo("/".toPath()))
            tr.parentFile()?.mkdirs()
            tr.createNewFile()

            tr.sink().buffer().use { out ->
                sys.source(entry).use { it.transfer(out) }
                out.flush()
            }
        }
    }
    return target
}
