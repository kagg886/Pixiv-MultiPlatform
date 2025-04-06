package top.kagg886.pmf.util

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromHexString
import moe.tarsin.gif.encodeGif
import top.kagg886.pixko.module.ugoira.UgoiraMetadata
import top.kagg886.pmf.backend.useTempDir
import top.kagg886.pmf.backend.useTempFile

const val UGOIRA_SCHEME = "pixiv-gif"

@OptIn(ExperimentalSerializationApi::class)
class UgoiraFetcher(
    private val data: Uri,
    private val diskCache: DiskCache,
    private val net: HttpClient,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val metadata = Cbor.decodeFromHexString<UgoiraMetadata>(data.path!!)
        val diskCacheKey = data.toString()
        val editor = diskCache.openEditor(diskCacheKey)!!
        val snapshot = runCatching {
            useTempFile { zip ->
                zip.writeBytes(net.get(metadata.url.content).bodyAsBytes())
                useTempDir { workDir ->
                    zip.unzip(workDir)
                    encodeGif(editor.data) {
                        for (i in metadata.frames) {
                            frame(path = workDir / i.file, delay = i.delay)
                        }
                    }
                }
            }
        }.fold(
            { editor.commitAndOpenSnapshot()!! },
            {
                editor.abort()
                throw it
            },
        )
        return with(snapshot) {
            SourceFetchResult(
                source = ImageSource(
                    file = data,
                    fileSystem = diskCache.fileSystem,
                    diskCacheKey = diskCacheKey,
                    closeable = this,
                ),
                mimeType = "image/gif",
                dataSource = DataSource.DISK,
            )
        }
    }

    class Factory(private val net: () -> HttpClient) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != UGOIRA_SCHEME) return null
            return UgoiraFetcher(data, imageLoader.diskCache!!, net())
        }
    }
}
