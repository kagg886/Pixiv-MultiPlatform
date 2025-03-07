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

actual fun LoginScreenViewModel.initKCEFLocal(file: Path): Job = intent {
    if (WEBVIEW_INSTALL_LOCK_PATH.exists()) {
        return@intent initKCEF().join()
    }
    if (file.name.endsWith(".tar.gz")) {
        postSideEffect(LoginSideEffect.Toast("必须是.tar.gz结尾的文件！"))
        return@intent
    }
    val state = LoginViewState.LoginType.BrowserLogin.Loading(MutableStateFlow("解压浏览器内核中..."))
    reduce { state }
    TarGzExtractor.extract(
        WEBVIEW_INSTALL_DIR,
        file.toFile(),
        4096
    )
    state.msg.tryEmit("安装浏览器内核中")
    TarGzExtractor.move(
        WEBVIEW_INSTALL_DIR
    )

    if (Platform.getCurrentPlatform().os.isMacOSX) {
        state.msg.tryEmit("正在设置xattr...")
        WEBVIEW_INSTALL_DIR.unquarantine()
    }
    state.msg.tryEmit("创建安装锁...")
    WEBVIEW_INSTALL_LOCK_PATH.createNewFile()

    if (!WEBVIEW_INSTALL_LOCK_PATH.exists()) {
        throw KCEFException.InstallationLock
    }
    return@intent initKCEF().join()
}
