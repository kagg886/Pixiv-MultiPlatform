package top.kagg886.filepicker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import top.kagg886.filepicker.internal.NativeFilePicker
import top.kagg886.filepicker.internal.initNativeLib
import top.kagg886.pmf.util.sink
import top.kagg886.pmf.util.source

actual object FilePicker {
    internal val nativeFilePicker by lazy {
        initNativeLib()
        NativeFilePicker()
    }
}

actual suspend fun FilePicker.openFileSaver(
    suggestedName: String,
    extension: String?,
    directory: Path?,
) = withContext(Dispatchers.Main) {
    val ptr = nativeFilePicker.openFileSaver(suggestedName, extension, directory?.toString())
    withContext(Dispatchers.IO) {
        nativeFilePicker.awaitFileSaver(ptr)?.toPath()?.sink()
    }
}

actual suspend fun FilePicker.openFilePicker(
    ext: List<String>?,
    title: String?,
    directory: Path?,
) = withContext(Dispatchers.Main) {
    val ptr = nativeFilePicker.openFilePicker(ext?.toTypedArray(), title, directory?.toString())
    withContext(Dispatchers.IO) {
        nativeFilePicker.awaitFilePicker(ptr)?.toPath()?.source()
    }
}

suspend fun FilePicker.openFolderPicker(
    title: String? = null,
    directory: Path? = null,
) = withContext(Dispatchers.Main) {
    val ptr = nativeFilePicker.openDictionaryPicker(title, directory?.toString())
    withContext(Dispatchers.IO) {
        nativeFilePicker.awaitDictionaryPicker(ptr)?.toPath()
    }
}
