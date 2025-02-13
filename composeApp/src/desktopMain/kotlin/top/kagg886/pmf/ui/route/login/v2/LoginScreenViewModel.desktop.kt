package top.kagg886.pmf.ui.route.login.v2

import dev.datlag.kcef.KCEF
import kotlinx.coroutines.runBlocking
import top.kagg886.pmf.backend.dataPath

@Suppress("DefaultLocale")
actual fun LoginScreenViewModel.initKCEF() = intent {
    KCEF.init(
        builder = {
            installDir(dataPath.resolve("web-view").toFile())
            progress {
                onDownloading {
                    runBlocking {
                        reduce {
                            LoginViewState.LoginType.BrowserLogin.Loading("download... ${String.format("%.2f", it)}")
                        }
                    }
                }

                onExtracting {
                    runBlocking {
                        reduce {
                            LoginViewState.LoginType.BrowserLogin.Loading("extracting...")
                        }
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
                    runBlocking {
                        reduce {
                            LoginViewState.LoginType.BrowserLogin.Loading("initializing...")
                        }
                    }
                }

                onInstall {
                    runBlocking {
                        reduce {
                            LoginViewState.LoginType.BrowserLogin.Loading("installing...")
                        }
                    }
                }
                onLocating {
                    runBlocking {
                        reduce {
                            LoginViewState.LoginType.BrowserLogin.Loading("locating...")
                        }
                    }
                }
            }
        },
        onError = {
            runBlocking {
                reduce {
                    LoginViewState.LoginType.BrowserLogin.Loading("初始化时发生错误!\n${it?.stackTraceToString()}")
                }
            }
        },
        onRestartRequired = {
            runBlocking {
                reduce {
                    LoginViewState.LoginType.BrowserLogin.Loading("请手动重启程序。")
                }
            }
        }
    )

}