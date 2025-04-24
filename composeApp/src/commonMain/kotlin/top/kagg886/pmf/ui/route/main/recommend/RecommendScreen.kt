package top.kagg886.pmf.ui.route.main.recommend

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.NavigationItem
import top.kagg886.pmf.composeWithAppBar
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchSideEffect
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchSideEffect
import top.kagg886.pmf.ui.util.collectSideEffect

class RecommendScreen : Screen {
    @Composable
    override fun Content() = NavigationItem.RECOMMEND.composeWithAppBar {
        RecommendScreen()
    }
}

@Composable
fun Screen.RecommendScreen() {
    val snackbarHostState = LocalSnackBarHost.current
    val page = rememberScreenModel {
        object : ScreenModel {
            val page = mutableIntStateOf(0)
        }
    }
    TabContainer(
        modifier = Modifier.fillMaxSize(),
        state = page.page,
        tab = listOf("插画", "小说"),
    ) {
        when (it) {
            0 -> {
                val nav = LocalNavigator.currentOrThrow
                val model = nav.koinNavigatorScreenModel<RecommendIllustViewModel>()
                model.collectSideEffect { effect ->
                    when (effect) {
                        is IllustFetchSideEffect.Toast -> {
                            snackbarHostState.showSnackbar(effect.msg)
                        }
                    }
                }
                IllustFetchScreen(model)
            }

            1 -> {
                val nav = LocalNavigator.currentOrThrow
                val model = nav.koinNavigatorScreenModel<RecommendNovelViewModel>()
                model.collectSideEffect { effect ->
                    when (effect) {
                        is NovelFetchSideEffect.Toast -> {
                            snackbarHostState.showSnackbar(effect.msg)
                        }
                    }
                }
                NovelFetchScreen(model)
            }
        }
    }
}
