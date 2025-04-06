package top.kagg886.pmf.util

import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.Options
import io.ktor.util.decodeBase64String

const val UGOIRA_SCHEME = "pixiv-gif"

class UgoiraFetcher(private val data: String) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val url = data.decodeBase64String()
        return null
    }

    object Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != UGOIRA_SCHEME) return null
            val path = data.path ?: return null
            return UgoiraFetcher(path)
        }
    }
}
