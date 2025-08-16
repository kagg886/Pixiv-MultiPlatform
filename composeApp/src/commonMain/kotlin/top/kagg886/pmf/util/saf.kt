package top.kagg886.pmf.util

import androidx.compose.runtime.Composable
import okio.FileSystem
import top.kagg886.filepicker.FilePicker

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/11 10:56
 * ================================================
 */

expect fun safFileSystem(uri: String): FileSystem
