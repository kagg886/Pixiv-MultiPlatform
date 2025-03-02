package top.kagg886.gif

import androidx.compose.ui.graphics.ImageBitmap
import okio.Source

expect fun Source.toImageBitmap():ImageBitmap
