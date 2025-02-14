package top.kagg886.pmf

import androidx.compose.ui.window.ComposeUIViewController
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startKoin0()
    SingletonSketch.setSafe {
        Sketch.Builder(PlatformContext.INSTANCE).applyCustomSketchConfig()
    }
    return ComposeUIViewController {
        App()
    }
}