package top.kagg886.pmf

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil3.ComponentRegistry
import coil3.gif.AnimatedImageDecoder
import okio.Path

actual fun shareFile(file: Path, name: String, mime: String) {
    with(PMFApplication.getApp()) {
        val cache = cacheDir.resolve("share").resolve(name)

        if (!cache.exists()) {
            cache.parentFile?.mkdirs()
            cache.createNewFile()
        }

        file.toFile().inputStream().use { i ->
            cache.outputStream().use { o ->
                i.copyTo(o)
            }
        }

        val intent = Intent("android.intent.action.SEND")
        intent.putExtra(
            "android.intent.extra.STREAM",
            FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                cache,
            ),
        )
        intent.flags = FLAG_ACTIVITY_NEW_TASK
        intent.type = mime
        startActivity(intent, null)
    }
}

actual suspend fun copyImageToClipboard(bitmap: ByteArray): Unit = throw UnsupportedOperationException()

actual fun openBrowser(link: String) {
    with(PMFApplication.getApp()) {
        val uri = link.toUri()
        startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                flags = FLAG_ACTIVITY_NEW_TASK
            },
        )
    }
}

actual fun ComponentRegistry.Builder.installGifDecoder() = add(AnimatedImageDecoder.Factory(false))
