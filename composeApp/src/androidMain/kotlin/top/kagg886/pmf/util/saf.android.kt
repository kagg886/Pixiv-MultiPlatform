package top.kagg886.pmf.util

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.Composable
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
import top.kagg886.pmf.util.logger

actual fun safFileSystem(uri: String): FileSystem = object : FileSystem() {
    private val context = PMFApplication.getApp()
    private val contentResolver: ContentResolver = context.contentResolver

    private val rootDocumentFile = DocumentFile.fromTreeUri(context, uri.toUri())
        ?: throw IllegalArgumentException("Invalid tree URI: $uri")

    init {
        logger.i("SAF FileSystem initialized with URI: $uri")
        logger.d("Root document exists: ${rootDocumentFile.exists()}, canRead: ${rootDocumentFile.canRead()}, canWrite: ${rootDocumentFile.canWrite()}")
    }

    private fun pathToDocumentFile(path: Path): DocumentFile? {
        logger.v("Looking for document file at path: $path")

        if (path.toString() == "/" || path.toString().isEmpty()) {
            logger.v("Returning root document file for path: $path")
            return rootDocumentFile
        }

        val segments = path.toString().removePrefix("/").split("/").filter { it.isNotEmpty() }
        logger.v("Path segments: $segments")
        var current = rootDocumentFile

        for (segment in segments) {
            current = current.findFile(segment) ?: run {
                logger.v("Segment '$segment' not found in path: $path")
                return null
            }
            logger.v("Found segment '$segment' in path: $path")
        }

        logger.v("Successfully resolved path: $path")
        return current
    }

    private fun createDocumentFile(path: Path, isDirectory: Boolean): DocumentFile? {
        logger.i("Creating ${if (isDirectory) "directory" else "file"} at path: $path")

        val segments = path.toString().removePrefix("/").split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) {
            logger.v("Empty segments, returning root document file")
            return rootDocumentFile
        }

        var current = rootDocumentFile

        // 创建所有父目录
        for (i in 0 until segments.size - 1) {
            val segment = segments[i]
            var child = current.findFile(segment)
            if (child == null) {
                logger.d("Creating parent directory: $segment")
                child = current.createDirectory(segment) ?: run {
                    logger.e("Failed to create parent directory: $segment")
                    return null
                }
            } else {
                logger.v("Parent directory already exists: $segment")
            }
            current = child
        }

        // 创建最终的文件或目录
        val fileName = segments.last()
        logger.d("Creating final ${if (isDirectory) "directory" else "file"}: $fileName")

        return if (isDirectory) {
            current.createDirectory(fileName)?.also {
                logger.i("Successfully created directory: $path")
            } ?: run {
                logger.e("Failed to create directory: $path")
                null
            }
        } else {
            current.createFile("application/octet-stream", fileName)?.also {
                logger.i("Successfully created file: $path")
            } ?: run {
                logger.e("Failed to create file: $path")
                null
            }
        }
    }

    override fun canonicalize(path: Path): Path {
        // DocumentFile 不支持符号链接，直接返回原路径
        return path
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        logger.v("Getting metadata for path: $path")

        val documentFile = pathToDocumentFile(path) ?: run {
            logger.v("Document file not found for path: $path")
            return null
        }

        // 检查是否有完整的文件系统权限
        if (!hasFullFileSystemPermissions()) {
            logger.w("No full file system permissions for metadata access: $path")
            return null
        }

        val metadata = FileMetadata(
            isRegularFile = documentFile.isFile,
            isDirectory = documentFile.isDirectory,
            size = if (documentFile.isFile) documentFile.length() else null,
            createdAtMillis = null, // DocumentFile 不提供创建时间
            lastModifiedAtMillis = documentFile.lastModified().takeIf { it > 0 },
            lastAccessedAtMillis = null, // DocumentFile 不提供访问时间
        )

        logger.v("Metadata for $path: isFile=${metadata.isRegularFile}, isDir=${metadata.isDirectory}, size=${metadata.size}")
        return metadata
    }

    /**
     * 检查是否有完整的文件系统权限（创建、修改、删除文件/文件夹）
     */
    private fun hasFullFileSystemPermissions(): Boolean {
        logger.v("Checking full file system permissions")

        return try {
            // 检查根目录是否可访问
            if (!rootDocumentFile.exists() || !rootDocumentFile.canRead()) {
                logger.w("Root document file does not exist or cannot be read")
                return false
            }

            // 检查是否有写权限（能创建和删除）
            if (!rootDocumentFile.canWrite()) {
                logger.w("Root document file cannot be written")
                return false
            }

            // 尝试创建一个临时文件来测试权限
            val testFileName = ".pmf_permission_test_${System.currentTimeMillis()}"
            logger.v("Creating test file: $testFileName")
            val testFile = rootDocumentFile.createFile("text/plain", testFileName)

            if (testFile != null) {
                // 测试成功，删除测试文件
                logger.v("Test file created successfully, deleting it")
                val deleteResult = testFile.delete()
                logger.v("Test file deletion result: $deleteResult")
                return true
            } else {
                logger.w("Failed to create test file for permission check")
                return false
            }
        } catch (e: SecurityException) {
            logger.w("SAF permission check failed due to SecurityException", e)
            // 权限不足
            false
        } catch (e: Exception) {
            logger.w("SAF permission check failed due to unexpected exception", e)
            // 其他异常也认为权限不足
            false
        }
    }

    override fun list(dir: Path): List<Path> = listOrNull(dir) ?: throw FileNotFoundException("Directory not found: $dir")

    override fun listOrNull(dir: Path): List<Path>? {
        logger.v("Listing directory: $dir")

        // 检查权限
        if (!hasFullFileSystemPermissions()) {
            logger.w("No full file system permissions for listing directory: $dir")
            return null
        }

        val documentFile = pathToDocumentFile(dir) ?: run {
            logger.v("Document file not found for directory: $dir")
            return null
        }

        if (!documentFile.isDirectory) {
            logger.v("Path is not a directory: $dir")
            return null
        }

        return try {
            val files = documentFile.listFiles().mapNotNull { child ->
                child.name?.let { name ->
                    if (dir.toString() == "/") {
                        "/$name".toPath()
                    } else {
                        "$dir/$name".toPath()
                    }
                }
            }
            logger.v("Listed ${files.size} files in directory: $dir")
            files
        } catch (e: SecurityException) {
            logger.w("SecurityException while listing directory: $dir", e)
            null
        }
    }

    override fun openReadOnly(file: Path): FileHandle = throw UnsupportedOperationException("DocumentFile does not support FileHandle operations")

    override fun openReadWrite(
        file: Path,
        mustCreate: Boolean,
        mustExist: Boolean,
    ): FileHandle = throw UnsupportedOperationException("DocumentFile does not support FileHandle operations")

    override fun source(file: Path): Source {
        logger.d("Opening source for file: $file")

        // 检查权限
        if (!hasFullFileSystemPermissions()) {
            logger.e("Storage access permission revoked for source: $file")
            throw SecurityException("Storage access permission revoked for: $file")
        }

        val documentFile = pathToDocumentFile(file)
            ?: throw FileNotFoundException("File not found: $file").also {
                logger.e("File not found for source: $file")
            }

        if (!documentFile.isFile) {
            logger.e("Path is not a file for source: $file")
            throw IOException("Path is not a file: $file")
        }

        val inputStream = try {
            contentResolver.openInputStream(documentFile.uri)
        } catch (e: SecurityException) {
            logger.e("SecurityException opening input stream for: $file", e)
            throw SecurityException("Storage access permission revoked for: $file", e)
        } ?: throw IOException("Cannot open input stream for: $file").also {
            logger.e("Cannot open input stream for: $file")
        }

        logger.d("Successfully opened source for file: $file")

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
        logger.d("Opening sink for file: $file, mustCreate: $mustCreate")

        // 检查权限
        if (!hasFullFileSystemPermissions()) {
            logger.e("Storage access permission revoked for sink: $file")
            throw SecurityException("Storage access permission revoked for: $file")
        }

        var documentFile = pathToDocumentFile(file)

        if (documentFile == null) {
            // 文件不存在，创建新文件
            logger.d("File does not exist, creating new file: $file")
            documentFile = createDocumentFile(file, false)
                ?: throw IOException("Cannot create file: $file - Permission denied or invalid URI").also {
                    logger.e("Failed to create file for sink: $file")
                }
        } else if (mustCreate) {
            logger.e("File already exists but mustCreate is true: $file")
            throw IOException("File already exists: $file")
        }

        if (!documentFile.isFile) {
            logger.e("Path is not a file for sink: $file")
            throw IOException("Path is not a file: $file")
        }

        val outputStream = try {
            contentResolver.openOutputStream(documentFile.uri, "wt")
        } catch (e: SecurityException) {
            logger.e("SecurityException opening output stream for: $file", e)
            throw SecurityException("Storage access permission revoked for: $file", e)
        } ?: throw IOException("Cannot open output stream for: $file").also {
            logger.e("Cannot open output stream for: $file")
        }

        logger.d("Successfully opened sink for file: $file")

        return object : Sink {
            override fun write(source: Buffer, byteCount: Long) {
                var remaining = byteCount
                while (remaining > 0) {
                    val buffer = ByteArray(minOf(remaining, 8192L).toInt())
                    val len = source.read(buffer)
                    if (len == -1) break
                    outputStream.write(buffer, 0, len)
                    remaining -= len
                }
                flush()
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
        logger.d("Opening appending sink for file: $file, mustExist: $mustExist")

        val documentFile = pathToDocumentFile(file) ?: if (mustExist) {
            logger.e("File not found but mustExist is true for appending sink: $file")
            throw FileNotFoundException("File not found: $file")
        } else {
            logger.d("File not found, creating new file for appending sink: $file")
            return sink(file, false)
        }

        if (!documentFile.isFile) {
            logger.e("Path is not a file for appending sink: $file")
            throw IOException("Path is not a file: $file")
        }

        val outputStream = contentResolver.openOutputStream(documentFile.uri, "wa")
            ?: throw IOException("Cannot open output stream for: $file").also {
                logger.e("Cannot open appending output stream for: $file")
            }

        logger.d("Successfully opened appending sink for file: $file")

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
        logger.d("Creating directory: $dir, mustCreate: $mustCreate")

        if (dir.toString() == ".") {
            logger.v("Skipping creation of current directory")
            return
        }

        // 检查权限
        if (!hasFullFileSystemPermissions()) {
            logger.e("Storage access permission revoked for creating directory: $dir")
            throw SecurityException("Storage access permission revoked for: $dir")
        }

        val existingFile = pathToDocumentFile(dir)

        if (existingFile != null) {
            if (mustCreate) {
                logger.e("Directory already exists but mustCreate is true: $dir")
                throw IOException("Directory already exists: $dir")
            }
            if (!existingFile.isDirectory) {
                logger.e("Path exists but is not a directory: $dir")
                throw IOException("Path exists but is not a directory: $dir")
            }
            logger.v("Directory already exists: $dir")
            return
        }

        val createdFile = try {
            createDocumentFile(dir, true)
        } catch (e: SecurityException) {
            logger.e("SecurityException creating directory: $dir", e)
            throw SecurityException("Storage access permission revoked for: $dir", e)
        } ?: throw IOException("Cannot create directory: $dir - Permission denied or invalid URI").also {
            logger.e("Failed to create directory: $dir")
        }

        if (!createdFile.isDirectory) {
            logger.e("Created file is not a directory: $dir")
            throw IOException("Failed to create directory: $dir")
        }

        logger.i("Successfully created directory: $dir")
    }

    override fun atomicMove(source: Path, target: Path) {
        logger.i("Atomic move from $source to $target")

        // DocumentFile 不支持原子移动操作，使用复制+删除的方式
        val sourceFile = pathToDocumentFile(source)
            ?: throw FileNotFoundException("Source file not found: $source").also {
                logger.e("Source file not found for atomic move: $source")
            }

        if (pathToDocumentFile(target) != null) {
            logger.e("Target already exists for atomic move: $target")
            throw IOException("Target already exists: $target")
        }

        if (sourceFile.isDirectory) {
            logger.d("Atomic move: source is directory, recursively copying: $source")
            // 递归复制目录
            createDirectory(target, true)
            val sourceChildren = listOrNull(source) ?: emptyList()
            logger.d("Atomic move: copying ${sourceChildren.size} children from $source")
            for (child in sourceChildren) {
                val childName = child.name
                val targetChild = target / childName
                atomicMove(child, targetChild)
            }
        } else {
            logger.d("Atomic move: source is file, copying: $source")
            // 复制文件
            val targetFile = createDocumentFile(target, false)
                ?: throw IOException("Cannot create target file: $target").also {
                    logger.e("Cannot create target file for atomic move: $target")
                }

            contentResolver.openInputStream(sourceFile.uri)?.use { input ->
                contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                    input.copyTo(output)
                    logger.d("File content copied successfully from $source to $target")
                }
            } ?: throw IOException("Cannot copy file content").also {
                logger.e("Cannot copy file content from $source to $target")
            }
        }

        // 删除源文件
        logger.d("Deleting source file after copy: $source")
        if (!sourceFile.delete()) {
            logger.e("Cannot delete source file after atomic move: $source")
            throw IOException("Cannot delete source file: $source")
        }

        logger.i("Atomic move completed successfully from $source to $target")
    }

    override fun delete(path: Path, mustExist: Boolean) {
        logger.d("Deleting path: $path, mustExist: $mustExist")

        // 检查权限
        if (!hasFullFileSystemPermissions()) {
            logger.e("Storage access permission revoked for delete: $path")
            throw SecurityException("Storage access permission revoked for: $path")
        }

        val documentFile = pathToDocumentFile(path)

        if (documentFile == null) {
            if (mustExist) {
                logger.e("File not found but mustExist is true: $path")
                throw FileNotFoundException("File not found: $path")
            }
            logger.v("File not found but mustExist is false, skipping delete: $path")
            return
        }

        val deleteResult = try {
            documentFile.delete()
        } catch (e: SecurityException) {
            logger.e("SecurityException deleting file: $path", e)
            throw SecurityException("Storage access permission revoked for: $path", e)
        }

        if (!deleteResult) {
            logger.e("Delete operation failed: $path")
            throw IOException("Cannot delete: $path - Permission denied or file in use")
        }

        logger.i("Successfully deleted: $path")
    }

    override fun createSymlink(source: Path, target: Path): Unit = throw UnsupportedOperationException("DocumentFile does not support symbolic links")
}
