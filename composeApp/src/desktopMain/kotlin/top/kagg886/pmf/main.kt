package top.kagg886.pmf

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.*
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import org.jetbrains.compose.resources.painterResource
import top.kagg886.pmf.ui.route.crash.CrashApp
import java.awt.event.WindowEvent
import kotlin.system.exitProcess

//@OptIn(ExperimentalComposeUiApi::class)
//private val mainExceptionHandler = WindowExceptionHandlerFactory { _ ->
//    val openLock = ReentrantLock()
//    var isOpen = false
//    WindowExceptionHandler { ex ->
//        openLock.withLock {
//            if (!isOpen) {
//                isOpen = true
//                thread(isDaemon = true) {
//                    application {
//                        Window(
//                            onCloseRequest = ::exitApplication,
//                            title = "Crash",
//                            icon = painterResource(Res.drawable.kotlin),
//                        ) {
//                            CrashApp(throwable = ex)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//private val exceptionHandler = Thread.UncaughtExceptionHandler { _, ex ->
//    SwingUtilities.invokeLater {
//        throw ex
//    }
//}


@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin0()
    SingletonSketch.setSafe {
        Sketch.Builder(PlatformContext.INSTANCE).applyCustomSketchConfig()
    }

    var lastException by mutableStateOf<Throwable?>(null)

    application(exitProcessOnExit = false) {
        LaunchedEffect(Unit) {
            Thread.setDefaultUncaughtExceptionHandler { _, ex ->
                lastException = ex
                exitApplication()
            }
        }
        CompositionLocalProvider(
            LocalWindowExceptionHandlerFactory provides WindowExceptionHandlerFactory {window->
                WindowExceptionHandler { ex ->
                    lastException = ex
                    window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
                    throw ex
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

    if  (lastException != null) {
        singleWindowApplication {
            CrashApp(throwable = lastException!!)
        }
        exitProcess(1)
    }
    exitProcess(0)
}