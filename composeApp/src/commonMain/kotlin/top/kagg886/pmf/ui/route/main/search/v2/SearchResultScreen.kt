package top.kagg886.pmf.ui.route.main.search.v2

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.*

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

        val tab = rememberSaveable {
            mutableStateOf(0)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("[${state.keyword.joinToString(" ")}]的搜索结果", maxLines = 1)
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
            val data = listOfNotNull<Pair<String, @Composable (() -> Unit)>>(
                state.illustRepo?.let { "插画" to { IllustFetchScreen(it) } },
                state.novelRepo?.let { "小说" to { NovelFetchScreen(it) } },
                state.authorRepo?.let { "用户" to { AuthorFetchScreen(it) } },
            )

            TabContainer(
                state = tab,
                tab = data.map { pair -> pair.first },
                modifier = Modifier.padding(paddingValues),
            ) { index ->
                data[index].second.invoke()
            }
        }
    }
}
