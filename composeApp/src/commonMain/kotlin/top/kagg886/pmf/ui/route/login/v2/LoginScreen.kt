package top.kagg886.pmf.ui.route.login.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
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
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.NavigationItem
import top.kagg886.pmf.backend.PlatformEngine
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.guide.GuideScaffold
import top.kagg886.pmf.ui.util.withClickable
import top.kagg886.pmf.ui.util.withLink
import top.kagg886.pmf.util.setText
import top.kagg886.pmf.util.stringResource

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
                    navigator.replace(NavigationItem.RECOMMEND())
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
                        Text(stringResource(Res.string.login_wizard))
                    },
                    subTitle = {},
                    confirmButton = {
                        Button(
                            onClick = {
                                model.selectLoginType(LoginType.BrowserLogin)
                            },
                        ) {
                            Text(stringResource(Res.string.use_browser_login))
                        }
                    },
                    skipButton = {
                        TextButton(
                            onClick = {
                                model.selectLoginType(LoginType.InputTokenLogin)
                            },
                        ) {
                            Text(stringResource(Res.string.use_token_login))
                        }
                    },
                    content = {
                        Text(
                            stringResource(Res.string.login_guide),
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
                                    Text(stringResource(Res.string.confirm))
                                }
                            },
                            dismissButton = {
                                val uri = LocalUriHandler.current
                                TextButton(
                                    onClick = {
                                        uri.openUri("https://pmf.kagg886.top/docs/main/login.html#3-%E6%88%91%E8%AF%A5%E5%A6%82%E4%BD%95%E5%AF%BC%E5%87%BA%E7%99%BB%E5%BD%95token")
                                    },
                                ) {
                                    Text(stringResource(Res.string.help))
                                }
                            },
                            title = {
                                Text(stringResource(Res.string.token_login))
                            },
                            text = {
                                TextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    label = {
                                        Text(stringResource(Res.string.input_token))
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
                                        Text(stringResource(Res.string.warning))
                                    },
                                    subTitle = {
                                        Text(stringResource(Res.string.browser_init_failed))
                                    },
                                    skipButton = {
                                        TextButton(
                                            onClick = {
                                                model.installKCEFLocal()
                                            },
                                        ) {
                                            Text(stringResource(Res.string.choose_zip_path))
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                model.selectLoginType(LoginType.BrowserLogin)
                                            },
                                        ) {
                                            Text(stringResource(Res.string.retry))
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
                                                    Text(stringResource(Res.string.error_details))
                                                },
                                                text = {
                                                    Text(state.exception.stackTraceToString(), modifier = Modifier.verticalScroll(rememberScrollState()))
                                                },
                                                confirmButton = {
                                                    val clip = LocalClipboard.current
                                                    val scope = rememberCoroutineScope()
                                                    TextButton(
                                                        onClick = {
                                                            scope.launch {
                                                                clip.setText(
                                                                    state.exception.stackTraceToString(),
                                                                )
                                                            }
                                                        },
                                                    ) {
                                                        Text(stringResource(Res.string.copy_to_clipboard))
                                                    }
                                                },
                                            )
                                        }
                                        val fronted = stringResource(Res.string.browser_init_failed_msg_fronted)
                                        val thisLink = stringResource(Res.string.browser_init_failed_msg_this_link)
                                        val back = stringResource(Res.string.browser_init_failed_msg_backend)
                                        val clickable = stringResource(Res.string.browser_init_failed_msg_clickable)
                                        Text(
                                            buildAnnotatedString {
                                                append(fronted)
                                                withLink(theme, "https://pmf.kagg886.top/docs/main/login.html#3-%E7%99%BB%E5%BD%95%E7%9A%84%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98", thisLink)
                                                append(back)
                                                withClickable(theme, clickable) {
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
//    val state = rememberWebViewState("https://useragent.buyaocha.com/")
    // 不能使用LaunchedEffect，初始化后不能修改UA
    // issue: https://github.com/KevinnZou/compose-webview-multiplatform/issues/142
    state.webSettings.customUserAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36 EdgA/135.0.0.0"
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
