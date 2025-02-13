package top.kagg886.pmf.util

import okio.FileSystem
import okio.Path
import okio.SYSTEM

fun Path.sink() = FileSystem.SYSTEM.sink(this)

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