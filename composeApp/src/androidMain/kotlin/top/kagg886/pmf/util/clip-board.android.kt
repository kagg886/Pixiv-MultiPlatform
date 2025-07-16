package top.kagg886.pmf.util

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

actual suspend fun Clipboard.setText(text: String) = setClipEntry(
    ClipEntry(
        clipData = ClipData.newPlainText("", text),
    ),
)
