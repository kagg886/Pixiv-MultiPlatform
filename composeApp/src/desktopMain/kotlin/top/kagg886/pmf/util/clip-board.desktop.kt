package top.kagg886.pmf.util

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import java.awt.datatransfer.StringSelection

actual suspend fun Clipboard.setText(text: String) = setClipEntry(
    ClipEntry(
        nativeClipEntry = StringSelection(text),
    ),
)
