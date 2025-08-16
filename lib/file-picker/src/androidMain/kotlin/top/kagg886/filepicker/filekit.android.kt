@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.filepicker

import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.registry
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import okio.Buffer
import okio.Path
import okio.Sink
import okio.Source
import okio.Timeout

actual object FilePicker {
    fun init(activity: ComponentActivity) = FileKit.init(activity)
}

actual suspend fun FilePicker.openFileSaver(
    suggestedName: String,
    extension: String?,
    directory: Path?,
): Sink? {
    val file = FileKit.openFileSaver(
        suggestedName = suggestedName,
        extension = extension,
        directory = directory?.let { PlatformFile(it.toFile()) },
    )

    if (file == null) {
        return null
    }

    val sink = file.sink().buffered()
    return object : Sink {
        override fun write(source: Buffer, byteCount: Long) {
            sink.write(source.readByteArray(byteCount))
        }

        override fun flush() = sink.flush()

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = sink.close()
    }
}

actual suspend fun FilePicker.openFilePicker(
    ext: List<String>?,
    title: String?,
    directory: Path?,
): Source? {
    val file = FileKit.openFilePicker(
        type = FileKitType.File(extensions = ext?.toSet()),
        title = title,
        directory = directory?.let { PlatformFile(it.toFile()) },
    )

    if (file == null) {
        return null
    }

    val source = file.source().buffered()

    return object : Source {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val buffer = ByteArray(byteCount.toInt())
            val len = source.readAtMostTo(buffer)

            if (len != -1) {
                sink.write(buffer, 0, len)
            }

            return len.toLong()
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = source.close()
    }
}

suspend fun FilePicker.openFolderPicker() = withContext(Dispatchers.IO) {
    // Throw exception if registry is not initialized
    val registry = FileKit.registry

    // It doesn't really matter what the key is, just that it is unique
    val key = UUID.randomUUID().toString()

    suspendCoroutine { continuation ->
        val contract = ActivityResultContracts.OpenDocumentTree()
        val launcher = registry.register(key, contract) { treeUri ->
            val platformDirectory = treeUri?.let {
                FileKit.context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                // Transform the treeUri to a documentUri
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
                documentUri
            }
            continuation.resume(platformDirectory)
        }
        launcher.launch(null)
    }
}
