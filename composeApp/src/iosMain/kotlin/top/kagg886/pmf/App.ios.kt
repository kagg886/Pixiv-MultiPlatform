package top.kagg886.pmf

import kotlinx.coroutines.runBlocking
import okio.Path
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openBrowser(link: String): Unit = runBlocking {
    UIApplication.sharedApplication.openURL(
        url = NSURL.URLWithString(link)!!,
        options = mapOf<Any?, String>(),
        completionHandler = null
    )
}

actual fun shareFile(file: Path, name: String, mime: String): Unit = TODO()

actual suspend fun copyImageToClipboard(bitmap: ByteArray) {
    throw UnsupportedOperationException()
}
