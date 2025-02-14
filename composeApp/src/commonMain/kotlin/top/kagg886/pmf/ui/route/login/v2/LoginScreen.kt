package top.kagg886.pmf.ui.route.login.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
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
import top.kagg886.pmf.backend.PlatformEngine
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.guide.GuideScaffold
import top.kagg886.pmf.ui.route.main.recommend.RecommendScreen
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

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
                    navigator.replace(RecommendScreen())
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
    if (a is LoginViewState.LoginType.BrowserLogin.Loading) {
        Loading(text = a.msg)
        return
    }
    AnimatedContent(
        targetState = a,
        modifier = Modifier.fillMaxSize()
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
                            }
                        ) {
                            Text("使用嵌入式浏览器登录")
                        }
                    },
                    skipButton = {
                        TextButton(
                            onClick = {
                                model.selectLoginType(LoginType.InputTokenLogin)
                            }
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
                            }
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
                                    }
                                ) {
                                    Text("确定")
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
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }

                    is LoginViewState.LoginType.BrowserLogin -> {
                        when (state) {
                            LoginViewState.LoginType.BrowserLogin.ShowBrowser -> {
                                WebViewLogin(model)
                            }

                            is LoginViewState.LoginType.BrowserLogin.Loading -> error("can't walk to this branch")
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
                navigator: WebViewNavigator
            ): WebRequestInterceptResult {
                if (request.url.startsWith("pixiv://")) {
                    model.challengePixivLoginUrl(auth, request.url)
                    return WebRequestInterceptResult.Reject
                }
                return WebRequestInterceptResult.Allow
            }

        }
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
            navigator = webNav
        )
    }
}
