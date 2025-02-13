package top.kagg886.pmf

import okio.Path
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openBrowser(link: String) {
    UIApplication.sharedApplication.openURL(
        NSURL.URLWithString(link)!!
    )
}

actual fun shareFile(file: Path, name: String, mime: String): Unit = TODO()

actual suspend fun copyImageToClipboard(bitmap: ByteArray): Unit = TODO()