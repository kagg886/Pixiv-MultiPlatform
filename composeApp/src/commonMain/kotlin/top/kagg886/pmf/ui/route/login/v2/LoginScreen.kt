package top.kagg886.pmf.ui.route.login.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.buildAnnotatedString
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.*
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.NavigationItem
import top.kagg886.pmf.backend.PlatformEngine
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.guide.GuideScaffold
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.ui.util.withClickable
import top.kagg886.pmf.ui.util.withLink

class LoginScreen(clearOldSession: Boolean = false) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    init {
        if (clearOldSession) {
            PixivConfig.clear()
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { LoginScreenViewModel() }

        val snack = LocalSnackBarHost.current

        model.collectSideEffect {
            when (it) {
                LoginSideEffect.NavigateToMain -> {
                    navigator.replace(NavigationItem.RecommendScreen)
                }

                is LoginSideEffect.Toast -> {
                    snack.showSnackbar(it.msg)
                }
            }
        }

        val state by model.collectAsState()
        WaitLoginContent(state, model)
    }
}

@Composable
private fun WaitLoginContent(a: LoginViewState, model: LoginScreenViewModel) {
    AnimatedContent(
        targetState = a,
        modifier = Modifier.fillMaxSize(),
    ) { state ->
        when (state) {
            LoginViewState.WaitChooseLogin -> {
                GuideScaffold(
                    title = {
                        Text("登录向导")
                    },
                    subTitle = {},
                    confirmButton = {
                        Button(
                            onClick = {
                                model.selectLoginType(LoginType.BrowserLogin)
                            },
                        ) {
                            Text("使用嵌入式浏览器登录")
                        }
                    },
                    skipButton = {
                        TextButton(
                            onClick = {
                                model.selectLoginType(LoginType.InputTokenLogin)
                            },
                        ) {
                            Text("使用token登录")
                        }
                    },
                    content = {
                        Text(
                            buildAnnotatedString {
                                appendLine("        现在，我们要进行登录操作。")
                                appendLine("        Pixiv-MultiPlatform支持使用嵌入式浏览器登录和token登录。")
                                appendLine("        1. 嵌入式浏览器登录适用于第一次使用Pixiv-MultiPlatform的情况。在浏览器中输入账号密码后，程序就会自动解析token并进行登录操作。")
                                appendLine("注意：在电脑端使用嵌入式浏览器登录需要下载大约300M的Chromium内核。若您介意，请在手机登录后使用token登录。")
                                appendLine("        2. token登录适用于已经在手机里使用Pixiv-MultiPlatform但不想在电脑重复输入账号密码的情况。")
                            },
                        )
                    },
                )
            }

            is LoginViewState.LoginType -> {
                when (state) {
                    LoginViewState.LoginType.InputTokenLogin -> {
                        var text by remember {
                            mutableStateOf("")
                        }
                        AlertDialog(
                            onDismissRequest = {},
                            confirmButton = {
                                Button(
                                    onClick = {
                                        model.challengeRefreshToken(text)
                                    },
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                val uri = LocalUriHandler.current
                                TextButton(
                                    onClick = {
                                        uri.openUri("https://pmf.kagg886.top/main/login.html#3-%E6%88%91%E8%AF%A5%E5%A6%82%E4%BD%95%E5%AF%BC%E5%87%BA%E7%99%BB%E5%BD%95token")
                                    },
                                ) {
                                    Text("帮助")
                                }
                            },
                            title = {
                                Text("token登录")
                            },
                            text = {
                                TextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    label = {
                                        Text("请输入token")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                        )
                    }

                    is LoginViewState.LoginType.BrowserLogin -> {
                        when (state) {
                            LoginViewState.LoginType.BrowserLogin.ShowBrowser -> {
                                WebViewLogin(model)
                            }

                            is LoginViewState.LoginType.BrowserLogin.Loading -> {
                                val msg by state.msg.collectAsState()
                                Loading(text = msg)
                            }

                            is LoginViewState.LoginType.BrowserLogin.Error -> {
                                GuideScaffold(
                                    title = {
                                        Text("警告")
                                    },
                                    subTitle = {
                                        Text("无法初始化嵌入式浏览器")
                                    },
                                    skipButton = {
                                        TextButton(
                                            onClick = {
                                                model.installKCEFLocal()
                                            },
                                        ) {
                                            Text("选择压缩包路径")
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                model.selectLoginType(LoginType.BrowserLogin)
                                            },
                                        ) {
                                            Text("重试")
                                        }
                                    },
                                    content = {
                                        val theme = MaterialTheme.colorScheme
                                        var detailsDialog by remember {
                                            mutableStateOf(false)
                                        }
                                        if (detailsDialog) {
                                            AlertDialog(
                                                onDismissRequest = {
                                                    detailsDialog = false
                                                },
                                                title = {
                                                    Text("错误详情")
                                                },
                                                text = {
                                                    Text(state.exception.stackTraceToString(), modifier = Modifier.verticalScroll(rememberScrollState()))
                                                },
                                                confirmButton = {
                                                    val clip = LocalClipboardManager.current
                                                    TextButton(
                                                        onClick = {
                                                            clip.setText(
                                                                buildAnnotatedString {
                                                                    append(state.exception.stackTraceToString())
                                                                },
                                                            )
                                                        },
                                                    ) {
                                                        Text("复制")
                                                    }
                                                },
                                            )
                                        }
                                        Text(
                                            buildAnnotatedString {
                                                appendLine("由于一些未知的原因，无法初始化嵌入式浏览器。")
                                                appendLine("请关闭程序后尝试使用token登录。")
                                                append("或者参阅")
                                                withLink(theme, "https://pmf.kagg886.top/main/login.html#3-%E7%99%BB%E5%BD%95%E7%9A%84%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98", "此链接")
                                                append("的内容以手动安装嵌入式浏览器。")
                                                appendLine()
                                                appendLine()
                                                withClickable(theme, "点击此文本以查看详细信息") {
                                                    detailsDialog = true
                                                }
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            is LoginViewState.ProcessingUserData -> {
                Loading(text = state.msg)
            }
        }
    }
}

@Composable
private fun WebViewLogin(model: LoginScreenViewModel) {
    val auth = remember {
        PixivAccountFactory.newAccount(PlatformEngine)
    }
    val state = rememberWebViewState(auth.url)
    val webNav = rememberWebViewNavigator(
        requestInterceptor = object : RequestInterceptor {
            override fun onInterceptUrlRequest(
                request: WebRequest,
                navigator: WebViewNavigator,
            ): WebRequestInterceptResult {
                if (request.url.startsWith("pixiv://")) {
                    model.challengePixivLoginUrl(auth, request.url)
                    return WebRequestInterceptResult.Reject
                }
                return WebRequestInterceptResult.Allow
            }
        },
    )

    val progress = remember(state.loadingState) {
        when (state.loadingState) {
            is LoadingState.Loading -> (state.loadingState as LoadingState.Loading).progress
            else -> -1.0f
        }
    }

    Column {
        if (progress >= 0.0f && progress < 1.0f) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = state,
            navigator = webNav,
        )
    }
}
