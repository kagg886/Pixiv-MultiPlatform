package top.kagg886.pmf.ui.route.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.painterResource
import top.kagg886.pmf.BuildConfig
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.kotlin
import top.kagg886.pmf.ui.route.login.LoginScreen
import top.kagg886.pmf.ui.route.main.recommend.RecommendScreen
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

class WelcomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = navigator.koinNavigatorScreenModel<WelcomeModel>()
        val state by model.collectAsState()

        model.collectSideEffect {
            when (it) {
                WelcomeSideEffect.NavigateToMain -> {
                    val token = PixivConfig.pixiv_user
                    navigator.replace(
                        if (token == null) LoginScreen(true) else RecommendScreen()
                    )
                }
            }
        }
        WelcomeContent(state)
    }

    @Composable
    fun WelcomeContent(state0: WelcomeViewState) {
        val navigator = LocalNavigator.currentOrThrow
        val model = navigator.koinNavigatorScreenModel<WelcomeModel>()
        AnimatedContent(state0) { state ->
            when (state) {
                is WelcomeViewState.Welcome -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ElevatedCard(modifier = Modifier.fillMaxSize(0.6f)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Image(
                                    painter = painterResource(Res.drawable.kotlin),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("欢 迎 使 用", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(BuildConfig.APP_NAME)
                                Spacer(modifier = Modifier.weight(1f))
                                FloatingActionButton(
                                    onClick = {
                                        model.confirmInited()
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                        }
                    }
                }

                WelcomeViewState.Loading -> {}
            }
        }
    }

}