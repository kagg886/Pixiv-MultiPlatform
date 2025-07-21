package top.kagg886.filepicker

import javax.swing.SwingUtilities
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
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
): Source? {
    val deferred = CompletableDeferred<Source?>()

    withContext(Dispatchers.Main) {
        nativeFilePicker.openFilePicker(ext?.toTypedArray(), title, directory?.toString()) { path ->
            deferred.complete(path?.toPath()?.source())
        }
    }

    return deferred.await()
}
