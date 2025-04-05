package top.kagg886.pmf

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.UIKit.UIViewController

@Suppress("unused")
fun MainViewController(): UIViewController {
    setupEnv()
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(PlatformContext.INSTANCE).applyCustomConfig().build()
    }
    val keyStateFlow = MutableSharedFlow<KeyEvent>()
    return ComposeUIViewController {
        CompositionLocalProvider(
            LocalKeyStateFlow provides keyStateFlow,
            content = { App() },
        )
    }
}
