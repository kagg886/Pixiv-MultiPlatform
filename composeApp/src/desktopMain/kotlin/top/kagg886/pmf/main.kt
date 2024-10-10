package top.kagg886.pmf

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import org.jetbrains.compose.resources.painterResource


fun main() {
    startKoin0()
    SingletonSketch.setSafe {
        Sketch.Builder(PlatformContext.INSTANCE).applyCustomSketchConfig()
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = BuildConfig.APP_NAME,
            icon = painterResource(Res.drawable.kotlin),
        ) {
            App()
        }
    }
}