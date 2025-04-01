package top.kagg886.pmf.ui.route.main.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.*

class HistoryScreen : Screen {
    private class PageScreenModel : ScreenModel {
        val page: MutableState<Int> = mutableIntStateOf(0)
    }

    @Composable
    override fun Content() {
        val page = rememberScreenModel {
            PageScreenModel()
        }
        TabContainer(
            modifier = Modifier.fillMaxSize(),
            state = page.page,
            tab = listOf("插画", "小说"),
        ) {
            val snackbarHostState = LocalSnackBarHost.current
            when (it) {
                0 -> {
                    val nav = LocalNavigator.currentOrThrow
                    val model = nav.koinNavigatorScreenModel<HistoryIllustViewModel>()
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
                    val model = nav.koinNavigatorScreenModel<HistoryNovelViewModel>()
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
}
