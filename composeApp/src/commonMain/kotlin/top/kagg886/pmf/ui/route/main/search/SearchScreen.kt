package top.kagg886.pmf.ui.route.main.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchSort.*
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.SearchTarget.*
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

enum class SearchTab(val display: String) {
    ILLUST("插画"),
    NOVEL("小说"),
}

class SearchScreen(
    private val initialSort: SearchSort = DATE_DESC,
    private val initialTarget: SearchTarget = PARTIAL_MATCH_FOR_TAGS,
    private val initialKeyWords: String = "",
    val tab: SearchTab = SearchTab.ILLUST
) : Screen {
    private var illust_dirty = -1
    private var novel_dirty = -1


    override val key: ScreenKey by lazy {
        "Search_${initialKeyWords}_${initialTarget}_${initialSort}_${Random(System.currentTimeMillis()).nextInt()}"
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = navigator.koinNavigatorScreenModel<SearchViewModel>()
        val state by model.collectAsState()
        SearchScreenContent(state)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
    @Composable
    fun SearchScreenContent(state: SearchViewState) {
        val nav = LocalNavigator.currentOrThrow
        val model = nav.koinNavigatorScreenModel<SearchViewModel>()
        var active by remember { mutableStateOf(false) }
        val padding by animateDpAsState(if (active) 0.dp else 16.dp)

        var sort by mutableStateOf(initialSort)
        var target by mutableStateOf(initialTarget)
        var keyWords by remember { mutableStateOf(initialKeyWords) }

        var searchWords by remember { mutableStateOf(initialKeyWords) }

        LaunchedEffect(Unit) {
            snapshotFlow { keyWords }.debounce(1.seconds).distinctUntilChanged().collectLatest {
                model.searchTag(keyWords.split(" ").last())
            }
        }

        fun startSearch() {
            if (keyWords.isEmpty()) {
                return
            }
            with(Random(System.currentTimeMillis())) {
                illust_dirty = nextInt()
                novel_dirty = nextInt()
            }
            searchWords = keyWords
            active = false
        }
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                query = keyWords,
                onQueryChange = { keyWords = it },
                onSearch = { },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text("搜索") },
                leadingIcon = {
                    IconButton(
                        onClick = {
                            if (active) {
                                active = false
                                return@IconButton
                            }
                            nav.pop()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            startSearch()
                        },
                        enabled = keyWords.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            null
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = padding)
            ) {
                when (state) {
                    is SearchViewState.EmptySearch -> {
                        Column {
                            ListItem(
                                headlineContent = {
                                    Text("排序方式")
                                },
                                supportingContent = {
                                    Row {
                                        for (i in SearchSort.entries) {
                                            InputChip(
                                                selected = sort == i,
                                                onClick = {
                                                    sort = i
                                                },
                                                label = {
                                                    Text(
                                                        when (i) {
                                                            DATE_DESC -> "时间降序"
                                                            DATE_ASC -> "时间升序"
                                                            POPULAR_DESC -> "热度降序"
                                                        }
                                                    )
                                                },
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.height(15.dp))
                            ListItem(
                                headlineContent = {
                                    Text("搜索模式")
                                },
                                supportingContent = {
                                    Row {
                                        for (i in SearchTarget.entries) {
                                            InputChip(
                                                selected = target == i,
                                                onClick = {
                                                    target = i
                                                },
                                                label = {
                                                    Text(
                                                        when (i) {
                                                            EXACT_MATCH_FOR_TAGS -> "匹配精确tag"
                                                            PARTIAL_MATCH_FOR_TAGS -> "匹配模糊tag"
                                                            TITLE_AND_CAPTION -> "匹配标题简介"
                                                        }
                                                    )
                                                },
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }

                                }
                            )
                            Spacer(Modifier.height(15.dp))
                            ListItem(
                                headlineContent = {
                                    Text("热门tag")
                                },
                                supportingContent = {
                                    FlowRow {
                                        for (tag in state.tag) {
                                            AssistChip(
                                                onClick = {
                                                    nav.push(
                                                        SearchScreen(
                                                            sort, target, tag.tag.name
                                                        )
                                                    )
                                                },
                                                label = {
                                                    Column {
                                                        Text(tag.tag.name)
                                                        tag.tag.translatedName?.let {
                                                            Text("($it)", style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    SearchViewState.NonLoading -> {
                        Loading()
                    }

                    is SearchViewState.KeyWordSearch -> {
                        LazyColumn {
                            for (item in state.key) {
                                item {
                                    ListItem(
                                        headlineContent = {
                                            Text(text = item.name)
                                        },
                                        supportingContent = {
                                            Text(item.translatedName ?: "")
                                        },
                                        modifier = Modifier.clickable {
                                            keyWords = (keyWords.split(" ").dropLast(1)
                                                .joinToString(" ") + " " + item.name + " ").trimStart()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (searchWords.isNotEmpty()) {
                val page = rememberScreenModel {
                    PageScreenModel(page = mutableIntStateOf(tab.ordinal))
                }
                TabContainer(
                    modifier = Modifier.fillMaxSize(),
                    state = page.page,
                    tab = SearchTab.entries.map { it.display }
                ) {
                    val snackbarHostState = LocalSnackBarHost.current
                    when (it) {
                        0 -> {
                            val resultModel =
                                rememberScreenModel(tag = "search_result_illust_${keyWords}_${target}_${sort}_${illust_dirty}") {
                                    SearchResultIllustModel(
                                        word = keyWords,
                                        searchTarget = target,
                                        sort = sort
                                    )
                                }
                            resultModel.collectSideEffect { effect ->
                                when (effect) {
                                    is IllustFetchSideEffect.Toast -> {
                                        snackbarHostState.showSnackbar(effect.msg)
                                    }
                                }
                            }
                            IllustFetchScreen(resultModel)
                        }

                        1 -> {
                            val resultModel =
                                rememberScreenModel(tag = "search_result_novel_${keyWords}_${target}_${sort}_${novel_dirty}") {
                                    SearchResultNovelModel(
                                        word = keyWords,
                                        searchTarget = target,
                                        sort = sort
                                    )
                                }
                            resultModel.collectSideEffect { effect ->
                                when (effect) {
                                    is NovelFetchSideEffect.Toast -> {
                                        snackbarHostState.showSnackbar(effect.msg)
                                    }
                                }
                            }
                            NovelFetchScreen(resultModel)
                        }
                    }
                }
                return
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("点击搜索框以进行搜索")
                    Text("快捷TAG放在了搜索框中")
                }
            }
        }
    }

    private class PageScreenModel(
        val page: MutableState<Int> = mutableIntStateOf(0)
    ) : ScreenModel
}