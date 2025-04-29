package top.kagg886.pmf.ui.route.main.search.v2

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.Res
import top.kagg886.pmf.illust
import top.kagg886.pmf.novel
import top.kagg886.pmf.search_result_for
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.AuthorFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.user

class SearchResultScreen(
    private val keyword: List<String>,
    private val sort: SearchSort,
    private val target: SearchTarget,
) : Screen {
    override val key: ScreenKey by lazy {
        "search_result_${keyword}_${sort}_$target"
    }

    @OptIn(ExperimentalMaterial3Api::class, InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        val model = rememberScreenModel { SearchResultViewModel(keyword, sort, target) }
        val state by model.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }

        model.collectSideEffect { sideEffect ->
            when (sideEffect) {
                is SearchResultSideEffect.Toast -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
            }
        }

        BackHandler(true) {
            navigator.pop()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(
                                Res.string.search_result_for,
                                state.keyword.joinToString(" ")
                            ), maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navigator.pop()
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    },
                )
            },
        ) { paddingValues ->
            val data = buildMap<String, (@Composable () -> Unit)> {
                state.illustRepo?.let {
                    put(stringResource(Res.string.illust), { IllustFetchScreen(it) })
                }
                state.novelRepo?.let {
                    put(stringResource(Res.string.novel), { NovelFetchScreen(it) })
                }
                state.authorRepo?.let {
                    put(stringResource(Res.string.user), { AuthorFetchScreen(it) })
                }
            }

            var tab by rememberSaveable {
                mutableStateOf(
                    data.keys.first()
                )
            }

            TabContainer(
                modifier = Modifier.padding(paddingValues),
                tab = data.keys.toList(),
                tabTitle = { Text(it) },
                current = tab,
                onCurrentChange = {tab = it},
            ) {
                data[tab]?.invoke()
            }
//            TabContainer(
//                state = tab,
//                tab = data.map { pair -> pair.first },
//                modifier = Modifier.padding(paddingValues),
//            ) { index ->
//                data[index].second.invoke()
//            }
        }
    }
}
