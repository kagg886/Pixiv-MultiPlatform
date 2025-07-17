package top.kagg886.filepicker

import androidx.activity.ComponentActivity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.utils.toPath
import kotlin.math.sin
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
    directory: Path?
): Sink? {
    val file = FileKit.openFileSaver(
        suggestedName = suggestedName,
        extension = extension,
        directory = directory?.let { PlatformFile(it.toFile()) }
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
    directory: Path?
): Source? {
    val file = FileKit.openFilePicker(
        type = FileKitType.File(extensions = ext?.toSet()),
        title = title,
        directory = directory?.let { PlatformFile(it.toFile()) }
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
                sink.write(buffer,0,len)
            }

            return len.toLong()
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = source.close()

    }
}
