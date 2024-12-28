package top.kagg886.pmf.backend

import java.io.File

actual val dataPath: File by lazy {
    File(System.getProperty("user.home")).resolve(".config").resolve("pmf")
}

actual val cachePath: File by lazy {
    dataPath.resolve("cache")
}