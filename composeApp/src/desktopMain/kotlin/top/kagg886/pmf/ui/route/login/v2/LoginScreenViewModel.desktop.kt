package top.kagg886.pmf.ui.route.login.v2

import dev.datlag.kcef.KCEF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import top.kagg886.pmf.backend.dataPath

@Suppress("DefaultLocale")
actual fun LoginScreenViewModel.initKCEF() = intent {
    val loading = LoginViewState.LoginType.BrowserLogin.Loading(MutableStateFlow("正在初始化嵌入式浏览器..."))
    reduce { loading }
    KCEF.init(
        builder = {
            installDir(dataPath.resolve("web-view").toFile())
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
