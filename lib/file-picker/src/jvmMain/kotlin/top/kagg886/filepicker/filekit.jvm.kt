package top.kagg886.filepicker

import javax.swing.SwingUtilities
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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
    directory: Path?
): Sink? = suspendCoroutine {
    SwingUtilities.invokeLater {
        val rtnPath = nativeFilePicker.openFileSaver(suggestedName,extension,directory?.toString())
        it.resume(rtnPath?.toPath()?.sink())
    }
}

actual suspend fun FilePicker.openFilePicker(
    ext: List<String>?,
    title: String?,
    directory: Path?
): Source? = suspendCoroutine {
    SwingUtilities.invokeLater {
        val rtnPath = nativeFilePicker.openFilePicker(ext?.toTypedArray(),title,directory?.toString())
        it.resume(rtnPath?.toPath()?.source())
    }
}
