package top.kagg886.pmf.backend

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo

sealed class Platform(open val name: String) {

    sealed class Android : Platform("android") {
        data object AndroidPhone : Android()
        data object AndroidPad : Android()
    }

    sealed class Desktop(override val name: String) : Platform(name) {
        data object Linux : Desktop("linux")
        data object Windows : Desktop("windows")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
val Platform.useWideScreenMode: Boolean
    @Composable
    get() = with(LocalWindowInfo.current.containerSize) {
        width.toFloat() / height > 1
    }

expect val currentPlatform: Platform