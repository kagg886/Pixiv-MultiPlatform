package top.kagg886.pmf

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.reflect.full.primaryConstructor

@Composable
actual fun AppScaffold(nav: Navigator, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = LocalSnackBarHost.current)
        }
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(it)) {
            if (NavigationItem.entries.any { item -> item.screenClass.isInstance(nav.lastItemOrNull) }) {
                NavigationRail {
                    SearchButton()
                    for (entry in NavigationItem.entries) {
                        NavigationRailItem(
                            selected = entry.screenClass.isInstance(nav.lastItemOrNull),
                            onClick = {
                                if (entry.screenClass.isInstance(nav.lastItemOrNull)) {
                                    return@NavigationRailItem
                                }
                                nav.push(entry.screenClass.primaryConstructor!!.call())
                            },
                            icon = {
                                Icon(imageVector = entry.icon, null)
                            },
                            label = {
                                Text(entry.title)
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    ProfileAvatar()
                }
            }
            content(Modifier.fillMaxSize())
        }

    }
}

actual fun shareFile(file: File, name: String, mime: String) {
    Desktop.getDesktop().open(file)
}

actual suspend fun copyImageToClipboard(bitmap: ByteArray) {
    withContext(Dispatchers.IO) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(
            TransferableImage(bitmap),
            DesktopClipBoardOwner
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

    override fun hashCode(): Int {
        return image.contentHashCode()
    }

}