package top.kagg886.pmf

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.ComposeUIViewController
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.UIKit.UIViewController

@Suppress("unused")
fun MainViewController(): UIViewController {
    setupEnv()
    SingletonSketch.setSafe {
        Sketch.Builder(PlatformContext.INSTANCE).applyCustomSketchConfig()
    }
    val keyStateFlow = MutableSharedFlow<KeyEvent>()
    return ComposeUIViewController {
        CompositionLocalProvider(
            LocalKeyStateFlow provides keyStateFlow,
            content = { App() }
        )
    }
}
