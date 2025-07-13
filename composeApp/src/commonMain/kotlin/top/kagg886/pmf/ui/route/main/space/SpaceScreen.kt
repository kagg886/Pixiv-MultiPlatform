package top.kagg886.pmf.ui.route.main.space

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
import top.kagg886.pmf.follow
import top.kagg886.pmf.latest
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchSideEffect
import top.kagg886.pmf.util.stringResource

class SpaceScreen : Screen {
    @Composable
    override fun Content() = NavigationItem.SPACE.composeWithAppBar {
        SpaceScreen()
    }
}

@Composable
private fun Screen.SpaceScreen() {
    val page = rememberScreenModel {
        object : ScreenModel {
            val page = mutableIntStateOf(0)
        }
    }
    val index by page.page
    val tab = listOf(Res.string.follow, Res.string.latest)
    TabContainer(
        modifier = Modifier.fillMaxSize(),
        tab = tab,
        tabTitle = { Text(stringResource(it)) },
        current = tab[index],
        onCurrentChange = { page.page.value = tab.indexOf(it) },
    ) {
        when (it) {
            Res.string.follow -> {
                val nav = LocalNavigator.currentOrThrow
                val model = nav.koinNavigatorScreenModel<SpaceIllustViewModel>()
                val snackbarHostState = LocalSnackBarHost.current
                model.collectSideEffect { effect ->
                    when (effect) {
                        is IllustFetchSideEffect.Toast -> {
                            snackbarHostState.showSnackbar(effect.msg)
                        }
                    }
                }
                IllustFetchScreen(model)
            }

            Res.string.latest -> {
                val nav = LocalNavigator.currentOrThrow
                val model = nav.koinNavigatorScreenModel<NewestIllustViewModel>()
                val snackbarHostState = LocalSnackBarHost.current
                model.collectSideEffect { effect ->
                    when (effect) {
                        is IllustFetchSideEffect.Toast -> {
                            snackbarHostState.showSnackbar(effect.msg)
                        }
                    }
                }
                IllustFetchScreen(model)
            }
        }
    }
}
