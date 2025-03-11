package top.kagg886.pmf.util

import okio.FileHandle
import okio.Sink

internal class FileHandleSink(private val fileHandle: FileHandle, private val sink: Sink) : Sink by sink {
    override fun close() {
        sink.close()
        fileHandle.close()
    }
}
