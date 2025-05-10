package top.kagg886.pmf

import coil3.ComponentRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import top.kagg886.pmf.util.AnimatedSkiaImageDecoder
import top.kagg886.pmf.util.absolutePath

val scope = CoroutineScope(Dispatchers.Main)
actual fun openBrowser(link: String) {
    UIApplication.sharedApplication.openURL(
        url = NSURL.URLWithString(link)!!,
        options = mapOf<Any?, String>(),
        completionHandler = null,
    )
}

actual fun shareFile(file: Path, name: String, mime: String) {
    val url = NSURL.fileURLWithPath(file.absolutePath().toString())

    val controller = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null,
    )

    scope.launch {
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            controller,
            true,
            null,
        )
    }
}

actual suspend fun copyImageToClipboard(bitmap: ByteArray): Unit = throw UnsupportedOperationException()
actual fun ComponentRegistry.Builder.installGifDecoder() = add(AnimatedSkiaImageDecoder.Factory)
