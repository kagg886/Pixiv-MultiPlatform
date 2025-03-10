package top.kagg886.pmf.ui.route.main.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        Scaffold(
            topBar = {
                if (useWideScreenMode) {
                    TopAppBar(
                        title = {
                            Text("历史记录")
                        }
                    )
                }
            }
        ) { padding ->
            val page = rememberScreenModel {
                PageScreenModel()
            }
            TabContainer(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = page.page,
                tab = listOf("插画", "小说")
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
}