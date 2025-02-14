package top.kagg886.pmf.backend

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

actual val dataPath: Path by lazy {
    File(System.getProperty("user.home")).resolve(".config").resolve("pmf").toOkioPath()
}

actual val cachePath: Path by lazy {
    dataPath.resolve("cache")
}
