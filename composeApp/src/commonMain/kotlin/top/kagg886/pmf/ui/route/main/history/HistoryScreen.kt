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
import top.kagg886.pmf.Res
import top.kagg886.pmf.illust
import top.kagg886.pmf.novel
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchSideEffect
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchSideEffect
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.util.stringResource

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
            tab = listOf(stringResource(Res.string.illust), stringResource(Res.string.novel)),
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
