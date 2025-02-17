package top.kagg886.pmf.util

import co.touchlab.kermit.Logger
import korlibs.io.file.std.createZipFromTreeTo
import korlibs.io.file.std.rootLocalVfs
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.runBlocking
import okio.*

fun Path.sink() = FileSystem.SYSTEM.sink(this)

fun Source.transfer(sink: Sink) {
    val buf = Buffer()
    var len: Long
    while (this.read(buf, 1024).also { len = it } != -1L) {
        sink.write(buf, len)
        buf.clear()
    }
}

fun Path.writeBytes(byteArray: ByteArray) = sink().buffer().write(byteArray).close()

fun Path.writeString(s: String) = writeBytes(s.encodeToByteArray())

fun Path.source() = FileSystem.SYSTEM.source(this)

fun Path.absolutePath() = this

fun Path.parentFile() = absolutePath().parent

fun Path.listFile() = FileSystem.SYSTEM.list(this)

fun Path.exists() = FileSystem.SYSTEM.exists(this)

fun Path.isDirectory() = FileSystem.SYSTEM.metadata(this).isDirectory

fun Path.mkdirs() = FileSystem.SYSTEM.createDirectories(this)

fun Path.mkdir() = FileSystem.SYSTEM.createDirectory(this)

fun Path.delete() = FileSystem.SYSTEM.delete(this)

fun Path.createNewFile() = sink().close()

fun Path.deleteRecursively() = FileSystem.SYSTEM.deleteRecursively(this)

fun Path.zip(target: Path = FileSystem.SYSTEM.canonicalize(this).parent!!.resolve("${this.name}.zip")): Path {
    val vfs = rootLocalVfs[absolutePath().toString()]
    return runBlocking {
        vfs.createZipFromTreeTo(
            zipFile = rootLocalVfs[target.absolutePath().toString()]
        )
        Logger.d("zip complete! target: $target")
        target
    }
}
