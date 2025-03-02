package top.kagg886.gif

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import okio.Source
import okio.buffer
import org.jetbrains.skia.Image

actual fun Source.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(buffer().readByteArray()).toComposeImageBitmap()
}
