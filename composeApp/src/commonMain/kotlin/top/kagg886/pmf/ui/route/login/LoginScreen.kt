package top.kagg886.pmf.ui.route.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.*
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.PixivConfig
import top.kagg886.pmf.backend.SystemConfig
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.route.main.recommend.RecommendScreen
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

class LoginScreen(val exitLogin: Boolean = false) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<LoginScreenViewModel>()

        LaunchedEffect(exitLogin) {
            if (exitLogin) {
                PixivConfig.clear()
                model.init()
            }
        }

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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pixiv登录") },
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (state) {
                is LoginViewState.Loading -> {
                    Loading(modifier = Modifier.padding(it), (state as LoginViewState.Loading).msg)
                }

                LoginViewState.WaitLogin -> WaitLoginContent(Modifier.padding(it))
            }
        }
    }
}

@Composable
private fun WaitLoginContent(modifier: Modifier) {
    val auth = remember {
        PixivAccountFactory.newAccount()
    }
    val nav = LocalNavigator.currentOrThrow
    val model = nav.koinNavigatorScreenModel<LoginScreenViewModel>()


    val state = rememberWebViewState(auth.url)
    val webNav = rememberWebViewNavigator(
        requestInterceptor = object :RequestInterceptor {
            override fun onInterceptUrlRequest(
                request: WebRequest,
                navigator: WebViewNavigator
            ): WebRequestInterceptResult {
                if (request.url.startsWith("pixiv://")) {
                    model.verifyPixivAccount(auth, request.url)
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
    Column(modifier = modifier.fillMaxSize()) {
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