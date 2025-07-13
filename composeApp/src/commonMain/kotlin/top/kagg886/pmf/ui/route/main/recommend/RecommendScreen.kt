package top.kagg886.pmf.ui.route.main.recommend

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.NavigationItem
import top.kagg886.pmf.Res
import top.kagg886.pmf.composeWithAppBar
import top.kagg886.pmf.illust
import top.kagg886.pmf.novel
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchSideEffect
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchSideEffect
import top.kagg886.pmf.util.stringResource

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
    val index by page.page
    val tab = listOf(Res.string.illust, Res.string.novel)
    TabContainer(
        modifier = Modifier.fillMaxSize(),
        tab = tab,
        tabTitle = { Text(stringResource(it)) },
        current = tab[index],
        onCurrentChange = { page.page.value = tab.indexOf(it) },
    ) {
        when (it) {
            Res.string.illust -> {
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

            Res.string.novel -> {
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
