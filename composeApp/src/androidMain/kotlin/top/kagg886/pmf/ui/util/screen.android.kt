package top.kagg886.pmf.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

actual val useWideScreenMode: Boolean
    @Composable
    get() = with(LocalConfiguration.current) {
        screenWidthDp.toFloat() / screenHeightDp.toFloat() > 1f
    }
