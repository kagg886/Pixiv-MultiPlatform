package top.kagg886.pmf.util

import co.touchlab.kermit.Logger
import korlibs.io.file.std.createZipFromTreeTo
import korlibs.io.file.std.uniVfs
import kotlinx.coroutines.runBlocking
import okio.Path

actual fun Path.zip(target: Path): Path = runBlocking {
    this@zip.absolutePath().toString().uniVfs.createZipFromTreeTo(
        zipFile = target.absolutePath().toString().uniVfs
    )
    Logger.d("zip complete! target: $target")
    target
}
