package top.kagg886.pmf

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.*
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import org.jetbrains.compose.resources.painterResource
import top.kagg886.pmf.ui.route.crash.CrashApp
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock


@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin0()
    SingletonSketch.setSafe {
        Sketch.Builder(PlatformContext.INSTANCE).applyCustomSketchConfig()
    }
    application {
        CompositionLocalProvider(
            LocalWindowExceptionHandlerFactory provides WindowExceptionHandlerFactory { window ->
                val openLock = ReentrantLock()
                var isOpen = false
                return@WindowExceptionHandlerFactory WindowExceptionHandler { ex->
                    openLock.withLock {
                        if (!isOpen) {
                            isOpen = true
                            thread(isDaemon = true) {
                                application {
                                    Window(
                                        onCloseRequest = ::exitApplication,
                                        title = "Crash",
                                        icon = painterResource(Res.drawable.kotlin),
                                    ) {
                                        CrashApp(throwable = ex)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {
            Window(
                onCloseRequest = ::exitApplication,
                title = BuildConfig.APP_NAME,
                icon = painterResource(Res.drawable.kotlin),
            ) {
                App()
            }
        }
    }
}