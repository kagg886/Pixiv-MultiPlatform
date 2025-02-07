package top.kagg886.pmf

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.*
import cafe.adriel.voyager.core.screen.Screen
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import top.kagg886.pmf.ui.route.crash.CrashApp
import top.kagg886.pmf.ui.route.welcome.WelcomeScreen
import kotlin.system.exitProcess


@OptIn(ExperimentalComposeUiApi::class)
fun launchApp(init: Screen = WelcomeScreen()) {
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
            LocalWindowExceptionHandlerFactory provides WindowExceptionHandlerFactory { window ->
                WindowExceptionHandler { ex ->
                    lastException = ex
                    exitApplication()
                }
            },
            LocalKeyStateFlow provides remember { MutableSharedFlow() }
        ) {
            CompositionLocalProvider {
                val flow = LocalKeyStateFlow.current as MutableSharedFlow
                val scope = rememberCoroutineScope()
                Window(
                    onCloseRequest = ::exitApplication,
                    title = BuildConfig.APP_NAME,
                    icon = painterResource(Res.drawable.kotlin),
                    onKeyEvent = {
                        scope.launch {
                            flow.emit(it)
                        }
                        true
                    }
                ) {
                    App(init)
                }
            }
        }
    }

    if (lastException != null) {
        singleWindowApplication {
            CrashApp(throwable = lastException!!.stackTraceToString())
        }
        exitProcess(1)
    }
    exitProcess(0)
}

fun main() = launchApp()