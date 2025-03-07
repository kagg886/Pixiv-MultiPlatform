@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.pmf.ui.route.login.v2

import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFException
import dev.datlag.kcef.Platform
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okio.Path
import top.kagg886.pmf.backend.dataPath
import dev.datlag.kcef.step.extract.TarGzExtractor
import dev.datlag.kcef.common.unquarantine

private val WEBVIEW_INSTALL_DIR = dataPath.resolve("web-view").toFile()
private val WEBVIEW_INSTALL_LOCK_PATH = WEBVIEW_INSTALL_DIR.resolve("install.lock")

@Suppress("DefaultLocale")
actual fun LoginScreenViewModel.initKCEF() = intent {
    val loading = LoginViewState.LoginType.BrowserLogin.Loading(MutableStateFlow("正在初始化嵌入式浏览器..."))
    reduce { loading }
    KCEF.init(
        builder = {
            installDir(WEBVIEW_INSTALL_DIR)
            progress {
                onDownloading {
                    loading.msg.tryEmit("下载浏览器内核中... ${String.format("%.2f", it)}")
                }

                onExtracting {
                    loading.msg.tryEmit("解压浏览器内核中...")
                }

                onInitialized {
                    runBlocking {
                        reduce {
                            LoginViewState.LoginType.BrowserLogin.ShowBrowser
                        }
                    }
                }

                onInitializing {
                    loading.msg.tryEmit("Initializing...")
                }

                onInstall {
                    loading.msg.tryEmit("Installing...")
                }
                onLocating {
                    loading.msg.tryEmit("Locating...")
                }
            }
        },
        onError = {
            runBlocking {
                reduce {
                    LoginViewState.LoginType.BrowserLogin.Error(it ?: Throwable("未知错误"))
                }
            }
        },
    )
}

actual fun LoginScreenViewModel.initKCEFLocal(file: Path): Job {
    if (WEBVIEW_INSTALL_LOCK_PATH.exists()) {
        return initKCEF()
    }
    if (file.name.endsWith(".tar.gz")) {
        throw IllegalArgumentException("file must be end with .tar.gz")
    }

    TarGzExtractor.extract(
        WEBVIEW_INSTALL_DIR,
        file.toFile(),
        4096
    )

    TarGzExtractor.move(
        WEBVIEW_INSTALL_DIR
    )

    if (Platform.getCurrentPlatform().os.isMacOSX) {
        WEBVIEW_INSTALL_DIR.unquarantine()
    }

    WEBVIEW_INSTALL_LOCK_PATH.createNewFile()

    if (!WEBVIEW_INSTALL_LOCK_PATH.exists()) {
        throw KCEFException.InstallationLock
    }
    return initKCEF()
}
