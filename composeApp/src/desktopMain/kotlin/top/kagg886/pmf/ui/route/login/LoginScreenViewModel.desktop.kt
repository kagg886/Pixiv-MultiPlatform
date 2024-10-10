package top.kagg886.pmf.ui.route.login

import dev.datlag.kcef.KCEF
import kotlinx.coroutines.runBlocking
import top.kagg886.pmf.backend.rootPath

@Suppress("DefaultLocale")
actual fun LoginScreenViewModel.initKCEF() = intent {
    KCEF.init(
        builder = {
            installDir(rootPath.resolve("web-view"))
            progress {
                onDownloading {
                    runBlocking {
                        reduce {
                            LoginViewState.Loading("download... ${String.format("%.2f", it)}")
                        }
                    }
                }

                onExtracting {
                    runBlocking {
                        reduce {
                            LoginViewState.Loading("extracting...")
                        }
                    }
                }

                onInitialized {
                    runBlocking {
                        reduce {
                            LoginViewState.WaitLogin
                        }
                    }
                }

                onInitializing {
                    runBlocking {
                        reduce {
                            LoginViewState.Loading("initializing...")
                        }
                    }
                }

                onInstall {
                    runBlocking {
                        reduce {
                            LoginViewState.Loading("installing...")
                        }
                    }
                }
                onLocating {
                    runBlocking {
                        reduce {
                            LoginViewState.Loading("locating...")
                        }
                    }
                }
            }
        },
        onError = {
            runBlocking {
                reduce {
                    LoginViewState.Loading("初始化时发生错误!\n${it?.stackTraceToString()}")
                }
            }
        },
        onRestartRequired = {
            runBlocking {
                reduce {
                    LoginViewState.Loading("请手动重启程序。")
                }
            }
        }
    )

}