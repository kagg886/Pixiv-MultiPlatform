package top.kagg886.pmf.util

import androidx.compose.runtime.Composable
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun safFileSystem(uri: String): FileSystem = object : ForwardingFileSystem(SYSTEM) {
    // 获取 iOS 沙盒的 Documents 目录作为根目录
    private val documentsPath: Path by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true,
        )
        paths.firstOrNull()?.toString()?.toPath()
            ?: throw IllegalStateException("Cannot access iOS Documents directory")
    }

    override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
        // 如果路径是绝对路径（以 / 开头），则映射到 Documents 目录
        return if (path.isAbsolute) {
            // 移除开头的 / 并拼接到 Documents 目录
            val relativePath = path.toString().removePrefix("/")
            if (relativePath.isEmpty()) {
                documentsPath
            } else {
                documentsPath / relativePath
            }
        } else {
            // 相对路径直接基于 Documents 目录
            documentsPath / path.toString()
        }
    }
}
