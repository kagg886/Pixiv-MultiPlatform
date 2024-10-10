package top.kagg886.pmf.backend

import java.io.File

actual val rootPath: File by lazy {
    File(System.getProperty("user.home")).resolve(".config").resolve("pmf")
}