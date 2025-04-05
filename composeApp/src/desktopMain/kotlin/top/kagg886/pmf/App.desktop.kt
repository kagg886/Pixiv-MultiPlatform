package top.kagg886.pmf

import coil3.ComponentRegistry
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.io.ByteArrayInputStream
import java.net.URI
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import top.kagg886.pmf.util.AnimatedSkiaImageDecoder

actual fun openBrowser(link: String) {
    Desktop.getDesktop().browse(URI.create(link))
}

actual fun shareFile(file: Path, name: String, mime: String) {
    Desktop.getDesktop().open(file.toFile())
}

actual suspend fun copyImageToClipboard(bitmap: ByteArray) {
    withContext(Dispatchers.IO) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(
            TransferableImage(bitmap),
            DesktopClipBoardOwner,
        )
    }
}

private object DesktopClipBoardOwner : ClipboardOwner {
    override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) = Unit
}

private data class TransferableImage(private val image: ByteArray) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = flavor in transferDataFlavors

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (flavor == DataFlavor.imageFlavor) {
            return ImageIO.read(ByteArrayInputStream(image))
        }
        throw UnsupportedFlavorException(flavor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransferableImage

        return image.contentEquals(other.image)
    }

    override fun hashCode(): Int = image.contentHashCode()
}

actual fun ComponentRegistry.Builder.installGifDecoder() = add(AnimatedSkiaImageDecoder.Factory())
