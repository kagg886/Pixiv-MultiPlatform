package top.kagg886.pmf.util

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual fun Path.zip(target: Path): Path = this.toFile().zip0(target.toFile()).toOkioPath()

private fun File.zip0(target: File): File {
    target.parentFile!!.mkdirs()
    target.createNewFile()
    val zip = ZipOutputStream(target.outputStream())
    zip.use {
        for (file in this.walkBottomUp()) {
            if (file.isDirectory) {
                break
            }
            it.putNextEntry(ZipEntry(file.relativeTo(this).path))
            it.write(file.readBytes())
            it.closeEntry()
        }
    }
    return target
}
