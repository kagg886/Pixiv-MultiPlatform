package top.kagg886.pmf.backend

import kotlin.uuid.Uuid
import okio.FileNotFoundException
import okio.Path
import top.kagg886.pmf.util.createNewFile
import top.kagg886.pmf.util.delete
import top.kagg886.pmf.util.deleteRecursively
import top.kagg886.pmf.util.mkdirs
import top.kagg886.pmf.util.parentFile

expect val dataPath: Path
expect val cachePath: Path

inline fun <T : Any> useTempFile(block: (Path) -> T): T {
    val file = cachePath.resolve(Uuid.random().toHexString())
    file.parentFile()?.mkdirs() ?: throw FileNotFoundException("parent file not found")
    file.createNewFile()

    try {
        return block(file)
    } finally {
        file.delete()
    }
}

inline fun <T : Any> useTempDir(block: (Path) -> T): T {
    val file = cachePath.resolve(Uuid.random().toHexString())
    file.mkdirs()

    try {
        return block(file)
    } finally {
        file.deleteRecursively()
    }
}
