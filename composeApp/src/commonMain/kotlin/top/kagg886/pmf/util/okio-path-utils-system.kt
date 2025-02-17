package top.kagg886.pmf.util

import co.touchlab.kermit.Logger
import korlibs.io.file.std.createZipFromTreeTo
import korlibs.io.file.std.rootLocalVfs
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.runBlocking
import okio.*

inline fun Path.sink() = FileSystem.SYSTEM.sink(this)

inline fun Source.transfer(sink: Sink) {
    val buf = Buffer()
    var len: Long
    while (this.read(buf, 1024).also { len = it } != -1L) {
        sink.write(buf, len)
        buf.clear()
    }
}

inline fun Path.writeBytes(byteArray: ByteArray) = sink().buffer().write(byteArray).close()

inline fun Path.writeString(s: String) = writeBytes(s.encodeToByteArray())

inline fun Path.source() = FileSystem.SYSTEM.source(this)

inline fun Path.absolutePath() = this

inline fun Path.parentFile() = absolutePath().parent

inline fun Path.listFile() = FileSystem.SYSTEM.list(this)

inline fun Path.exists() = FileSystem.SYSTEM.exists(this)

inline fun Path.isDirectory() = FileSystem.SYSTEM.metadata(this).isDirectory

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
        Logger.d("zip complete! target: $target")
        target
    }
}
