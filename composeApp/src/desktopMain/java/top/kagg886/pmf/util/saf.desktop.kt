package top.kagg886.pmf.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath

actual fun safFileSystem(uri: String): FileSystem = object : ForwardingFileSystem(SYSTEM) {
    // 将 uri 作为根目录路径
    private val rootPath = uri.toPath()

    override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
        // 如果路径是绝对路径（以 / 开头），则映射到指定的根目录
        return if (path.isAbsolute) {
            // 移除开头的 / 并拼接到根目录
            val relativePath = path.toString().removePrefix("/")
            if (relativePath.isEmpty()) {
                rootPath
            } else {
                rootPath / relativePath
            }
        } else {
            // 相对路径直接基于根目录
            rootPath / path.toString()
        }
    }
}
