package top.kagg886.pmf.util

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.FileNotFoundException
import java.io.IOException
import okio.Buffer
import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.Timeout
import top.kagg886.pmf.PMFApplication

actual fun safFileSystem(uri: String): FileSystem = object : FileSystem() {
    private val context = PMFApplication.getApp()
    private val contentResolver: ContentResolver = context.contentResolver

    private val rootDocumentFile = DocumentFile.fromTreeUri(context, uri.toUri())
        ?: throw IllegalArgumentException("Invalid tree URI: $uri")

    private fun pathToDocumentFile(path: Path): DocumentFile? {
        if (path.toString() == "/" || path.toString().isEmpty()) {
            return rootDocumentFile
        }

        val segments = path.toString().removePrefix("/").split("/").filter { it.isNotEmpty() }
        var current = rootDocumentFile

        for (segment in segments) {
            current = current.findFile(segment) ?: return null
        }

        return current
    }

    private fun createDocumentFile(path: Path, isDirectory: Boolean): DocumentFile? {
        val segments = path.toString().removePrefix("/").split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) return rootDocumentFile

        var current = rootDocumentFile

        // 创建所有父目录
        for (i in 0 until segments.size - 1) {
            val segment = segments[i]
            var child = current.findFile(segment)
            if (child == null) {
                child = current.createDirectory(segment) ?: return null
            }
            current = child
        }

        // 创建最终的文件或目录
        val fileName = segments.last()
        return if (isDirectory) {
            current.createDirectory(fileName)
        } else {
            current.createFile("application/octet-stream", fileName)
        }
    }
    override fun canonicalize(path: Path): Path {
        // DocumentFile 不支持符号链接，直接返回原路径
        return path
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        val documentFile = pathToDocumentFile(path) ?: return null

        return FileMetadata(
            isRegularFile = documentFile.isFile,
            isDirectory = documentFile.isDirectory,
            size = if (documentFile.isFile) documentFile.length() else null,
            createdAtMillis = null, // DocumentFile 不提供创建时间
            lastModifiedAtMillis = documentFile.lastModified().takeIf { it > 0 },
            lastAccessedAtMillis = null, // DocumentFile 不提供访问时间
        )
    }

    override fun list(dir: Path): List<Path> = listOrNull(dir) ?: throw FileNotFoundException("Directory not found: $dir")

    override fun listOrNull(dir: Path): List<Path>? {
        val documentFile = pathToDocumentFile(dir) ?: return null
        if (!documentFile.isDirectory) return null

        return documentFile.listFiles().mapNotNull { child ->
            child.name?.let { name ->
                if (dir.toString() == "/") {
                    "/$name".toPath()
                } else {
                    "$dir/$name".toPath()
                }
            }
        }
    }

    override fun openReadOnly(file: Path): FileHandle = throw UnsupportedOperationException("DocumentFile does not support FileHandle operations")

    override fun openReadWrite(
        file: Path,
        mustCreate: Boolean,
        mustExist: Boolean,
    ): FileHandle = throw UnsupportedOperationException("DocumentFile does not support FileHandle operations")

    override fun source(file: Path): Source {
        val documentFile = pathToDocumentFile(file)
            ?: throw FileNotFoundException("File not found: $file")

        if (!documentFile.isFile) {
            throw IOException("Path is not a file: $file")
        }

        val inputStream = contentResolver.openInputStream(documentFile.uri)
            ?: throw IOException("Cannot open input stream for: $file")

        return object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long {
                val buffer = ByteArray(minOf(byteCount, 8192L).toInt())
                val bytesRead = inputStream.read(buffer)
                return if (bytesRead == -1) {
                    -1L
                } else {
                    sink.write(buffer, 0, bytesRead)
                    bytesRead.toLong()
                }
            }

            override fun timeout(): Timeout = Timeout.NONE

            override fun close() {
                inputStream.close()
            }
        }
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        var documentFile = pathToDocumentFile(file)

        if (documentFile == null) {
            // 文件不存在，创建新文件
            documentFile = createDocumentFile(file, false)
                ?: throw IOException("Cannot create file: $file")
        } else if (mustCreate) {
            throw IOException("File already exists: $file")
        }

        if (!documentFile.isFile) {
            throw IOException("Path is not a file: $file")
        }

        val outputStream = contentResolver.openOutputStream(documentFile.uri, "wt")
            ?: throw IOException("Cannot open output stream for: $file")

        return object : Sink {
            override fun write(source: Buffer, byteCount: Long) {
                val buffer = ByteArray(byteCount.toInt())
                source.read(buffer)
                outputStream.write(buffer)
            }

            override fun flush() {
                outputStream.flush()
            }

            override fun timeout(): Timeout = Timeout.NONE

            override fun close() {
                outputStream.close()
            }
        }
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        val documentFile = pathToDocumentFile(file) ?: if (mustExist) {
            throw FileNotFoundException("File not found: $file")
        } else {
            return sink(file, false)
        }

        if (!documentFile.isFile) {
            throw IOException("Path is not a file: $file")
        }

        val outputStream = contentResolver.openOutputStream(documentFile.uri, "wa")
            ?: throw IOException("Cannot open output stream for: $file")

        return object : Sink {
            override fun write(source: Buffer, byteCount: Long) {
                val buffer = ByteArray(byteCount.toInt())
                source.read(buffer)
                outputStream.write(buffer)
            }

            override fun flush() {
                outputStream.flush()
            }

            override fun timeout(): Timeout = Timeout.NONE

            override fun close() {
                outputStream.close()
            }
        }
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        val existingFile = pathToDocumentFile(dir)

        if (existingFile != null) {
            if (mustCreate) {
                throw IOException("Directory already exists: $dir")
            }
            if (!existingFile.isDirectory) {
                throw IOException("Path exists but is not a directory: $dir")
            }
            return
        }

        val createdFile = createDocumentFile(dir, true)
            ?: throw IOException("Cannot create directory: $dir")

        if (!createdFile.isDirectory) {
            throw IOException("Failed to create directory: $dir")
        }
    }

    override fun atomicMove(source: Path, target: Path) {
        // DocumentFile 不支持原子移动操作，使用复制+删除的方式
        val sourceFile = pathToDocumentFile(source)
            ?: throw FileNotFoundException("Source file not found: $source")

        if (pathToDocumentFile(target) != null) {
            throw IOException("Target already exists: $target")
        }

        if (sourceFile.isDirectory) {
            // 递归复制目录
            createDirectory(target, true)
            val sourceChildren = listOrNull(source) ?: emptyList()
            for (child in sourceChildren) {
                val childName = child.name
                val targetChild = target / childName
                atomicMove(child, targetChild)
            }
        } else {
            // 复制文件
            val targetFile = createDocumentFile(target, false)
                ?: throw IOException("Cannot create target file: $target")

            contentResolver.openInputStream(sourceFile.uri)?.use { input ->
                contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Cannot copy file content")
        }

        // 删除源文件
        if (!sourceFile.delete()) {
            throw IOException("Cannot delete source file: $source")
        }
    }

    override fun delete(path: Path, mustExist: Boolean) {
        val documentFile = pathToDocumentFile(path)

        if (documentFile == null) {
            if (mustExist) {
                throw FileNotFoundException("File not found: $path")
            }
            return
        }

        if (!documentFile.delete()) {
            throw IOException("Cannot delete: $path")
        }
    }

    override fun createSymlink(source: Path, target: Path): Unit = throw UnsupportedOperationException("DocumentFile does not support symbolic links")
}
