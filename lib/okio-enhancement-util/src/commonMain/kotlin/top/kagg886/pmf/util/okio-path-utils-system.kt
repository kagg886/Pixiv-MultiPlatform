package top.kagg886.pmf.util

import korlibs.io.file.std.createZipFromTreeTo
import korlibs.io.file.std.rootLocalVfs
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

fun Path.meta() = FileSystem.SYSTEM.metadata(this)

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

fun Path.writeString(s: String) = writeBytes(s.encodeToByteArray())

fun Path.source() = FileSystem.SYSTEM.source(this)

fun Path.absolutePath() = FileSystem.SYSTEM.canonicalize("".toPath()).resolve(this).normalized()

fun Path.parentFile() = absolutePath().parent

fun Path.listFile() = FileSystem.SYSTEM.list(this)

fun Path.exists() = FileSystem.SYSTEM.exists(this)

fun Path.isDirectory() = meta().isDirectory

fun Path.mkdirs() = FileSystem.SYSTEM.createDirectories(this)

fun Path.mkdir() = FileSystem.SYSTEM.createDirectory(this)

fun Path.delete() = FileSystem.SYSTEM.delete(this)

fun Path.createNewFile() = sink().close()

fun Path.deleteRecursively() {
    FileSystem.SYSTEM.deleteRecursively(this)
    FileSystem.SYSTEM.delete(this)
}

suspend fun Path.zip(target: Path = FileSystem.SYSTEM.canonicalize(this).parent!!.resolve("${this.name}.zip")): Path {
    val vfs = rootLocalVfs[absolutePath().toString()]
    return target.apply {
        parentFile()?.mkdirs()
        createNewFile()
        vfs.createZipFromTreeTo(
            zipFile = rootLocalVfs[target.absolutePath().toString()]
        )
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
