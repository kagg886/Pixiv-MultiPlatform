package top.kagg886.pmf.util

import androidx.compose.ui.platform.Clipboard

expect suspend fun Clipboard.setText(text: String)
