package top.kagg886.pmf.util

import okio.FileSystem
import okio.Path
import okio.SYSTEM

fun Path.zip(target: Path = FileSystem.SYSTEM.canonicalize(this).parent!!.resolve("${this.name}.zip")): Path {
    TODO("MultiPlatform Not Support zip file!")
//    target.parentFile!!.mkdirs()
//    target.createNewFile()
//    val zip = ZipOutputStream(target.outputStream())
//    zip.use {
//        for (file in this.walkBottomUp()) {
//            if (file.isDirectory) {
//                break
//            }
//            it.putNextEntry(ZipEntry(file.relativeTo(this).path))
//            it.write(file.readBytes())
//            it.closeEntry()
//        }
//    }
//    return target
}