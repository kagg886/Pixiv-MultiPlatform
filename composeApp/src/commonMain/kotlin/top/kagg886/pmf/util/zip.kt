package top.kagg886.pmf.util

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun File.zip(target: File = File(this.parentFile,"${this.nameWithoutExtension}.zip")):File {
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