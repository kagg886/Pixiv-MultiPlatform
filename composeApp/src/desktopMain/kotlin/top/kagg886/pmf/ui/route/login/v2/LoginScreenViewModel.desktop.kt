@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.pmf.ui.route.login.v2

import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFException
import dev.datlag.kcef.Platform
import dev.datlag.kcef.common.unquarantine
import dev.datlag.kcef.step.extract.TarGzExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okio.Path
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.dataPath
import top.kagg886.pmf.browser_archive_should_be_tar_gz
import top.kagg886.pmf.creating_install_lock
import top.kagg886.pmf.downloading_browser
import top.kagg886.pmf.init_browser
import top.kagg886.pmf.installing_browser
import top.kagg886.pmf.unknown_error
import top.kagg886.pmf.unzipping_browser
import top.kagg886.pmf.util.absolutePath
import top.kagg886.pmf.xattr_setting

private val WEBVIEW_INSTALL_DIR = dataPath.resolve("web-view").toFile()
private val WEBVIEW_INSTALL_LOCK_PATH = WEBVIEW_INSTALL_DIR.resolve("install.lock")

@Suppress("DefaultLocale")
actual fun LoginScreenViewModel.initKCEF() = intent {
    val loading = LoginViewState.LoginType.BrowserLogin.Loading(MutableStateFlow(getString(Res.string.init_browser)))
    reduce { loading }
    KCEF.init(
        builder = {
            installDir(WEBVIEW_INSTALL_DIR)
            settings {
                logFile = top.kagg886.pmf.backend.cachePath.resolve("log").resolve("web-view.log").absolutePath().toString()
                cachePath = top.kagg886.pmf.backend.cachePath.resolve("web-view").absolutePath().toString()
            }
            progress {
                onDownloading {
                    intent {
                        loading.msg.tryEmit(getString(Res.string.downloading_browser, it))
                    }
                }

                onExtracting {
                    intent {
                        loading.msg.tryEmit(getString(Res.string.unzipping_browser))
                    }
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
                val defaultError = Throwable(getString(Res.string.unknown_error))
                reduce {
                    LoginViewState.LoginType.BrowserLogin.Error(it ?: defaultError)
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
        postSideEffect(LoginSideEffect.Toast(getString(Res.string.browser_archive_should_be_tar_gz)))
        return@intent
    }
    val state = LoginViewState.LoginType.BrowserLogin.Loading(MutableStateFlow(getString(Res.string.unzipping_browser)))
    reduce { state }
    TarGzExtractor.extract(
        WEBVIEW_INSTALL_DIR,
        file.toFile(),
        4096,
    )
    state.msg.tryEmit(getString(Res.string.installing_browser))
    TarGzExtractor.move(
        WEBVIEW_INSTALL_DIR,
    )

    if (Platform.getCurrentPlatform().os.isMacOSX) {
        state.msg.tryEmit(getString(Res.string.xattr_setting))
        WEBVIEW_INSTALL_DIR.unquarantine()
    }
    state.msg.tryEmit(getString(Res.string.creating_install_lock))
    WEBVIEW_INSTALL_LOCK_PATH.createNewFile()

    if (!WEBVIEW_INSTALL_LOCK_PATH.exists()) {
        throw KCEFException.InstallationLock
    }
    return@intent initKCEF().join()
}
