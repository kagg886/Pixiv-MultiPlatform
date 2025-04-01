package top.kagg886.pmf.backend

import java.io.File
import okio.Path
import okio.Path.Companion.toOkioPath

actual val dataPath: Path by lazy {
    File(System.getProperty("user.home")).resolve(".config").resolve("pmf").toOkioPath()
}

actual val cachePath: Path by lazy {
    dataPath.resolve("cache")
}
