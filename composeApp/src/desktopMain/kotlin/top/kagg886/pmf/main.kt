package top.kagg886.pmf

import androidx.compose.runtime.*
import androidx.compose.ui.window.*
import cafe.adriel.voyager.core.screen.Screen
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.route.crash.CrashApp
import top.kagg886.pmf.ui.route.welcome.WelcomeScreen

fun launchApp(init: () -> Screen = { WelcomeScreen() }) {
    setupEnv()
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(PlatformContext.INSTANCE).applyCustomConfig().build()
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
            LocalKeyStateFlow provides remember { MutableSharedFlow() },
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
                    },
                ) {
                    App(init())
                }
            }
        }
    }
    Logger.e("App exit with exception", lastException)
    if (lastException != null) {
        singleWindowApplication {
            CrashApp(throwable = lastException!!.stackTraceToString())
        }
        exitProcess(1)
    }
    exitProcess(0)
}

fun main() = launchApp()
