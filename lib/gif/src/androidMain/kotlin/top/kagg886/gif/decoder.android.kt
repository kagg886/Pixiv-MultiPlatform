package top.kagg886.gif

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import okio.Source
import okio.buffer

actual fun Source.toImageBitmap(): ImageBitmap {
    return BitmapFactory.decodeStream(this.buffer().inputStream()).asImageBitmap()
}
