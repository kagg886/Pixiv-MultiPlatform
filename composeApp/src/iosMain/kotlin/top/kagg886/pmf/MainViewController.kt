package top.kagg886.pmf

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.flow.MutableSharedFlow
import platform.UIKit.UIViewController
import platform.UIKit.navigationController
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.CrashConfig
import top.kagg886.pmf.ui.route.crash.CrashApp
import top.kagg886.pmf.util.logger

@OptIn(ExperimentalNativeApi::class)
@Suppress("unused")
fun MainViewController(): UIViewController {
    setupEnv()
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(PlatformContext.INSTANCE).applyCustomConfig().build()
    }

    val keyStateFlow = MutableSharedFlow<KeyEvent>()
    val controller = ComposeUIViewController {
        CompositionLocalProvider(
            LocalKeyStateFlow provides keyStateFlow
        ) {
            var hasUnResolveCrashInfo by remember {
                mutableStateOf(CrashConfig.hasUnResolveCrash)
            }
            if (hasUnResolveCrashInfo) {
                CrashApp(throwable = CrashConfig.crashText) {
                    CrashConfig.hasUnResolveCrash = false
                    CrashConfig.crashText = ""
                    hasUnResolveCrashInfo = false
                }
                return@CompositionLocalProvider
            }
            App()
        }
    }
    setUnhandledExceptionHook {
        controller.logger.e("App Crashed!",it)
        CrashConfig.hasUnResolveCrash = true
        CrashConfig.crashText = it.stackTraceToString()
    }
    return controller
}
