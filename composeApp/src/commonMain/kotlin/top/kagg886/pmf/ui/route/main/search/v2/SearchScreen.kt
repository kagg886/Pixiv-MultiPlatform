package top.kagg886.pmf.ui.route.main.search.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.dokar.chiptextfield.Chip
import com.dokar.chiptextfield.m3.ChipTextField
import com.dokar.chiptextfield.rememberChipTextFieldState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen
import top.kagg886.pmf.ui.route.main.search.v2.components.HistoryItem
import top.kagg886.pmf.ui.route.main.search.v2.components.SearchPropertiesPanel
import top.kagg886.pmf.ui.util.*
import kotlin.time.Duration.Companion.seconds

class SearchScreen(private val param: SearchParam = SearchParam.EmptySearch) : Screen {
    override val key: ScreenKey = param.hashCode().toString()

    @Composable
    override fun Content() {
        val model = rememberScreenModel(tag = param.toString()) { SearchViewModel(param) }
        val state by model.collectAsState()

        SearchScreenContent(model, state)
    }
}

sealed interface SearchParam {
    data object EmptySearch : SearchParam
    data class KeyWordSearch(val tag: List<String>, val sort: SearchSort, val target: SearchTarget) : SearchParam
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
private fun SearchScreenContent(model: SearchViewModel, state: SearchViewState) {
    when (state) {
        is SearchViewState.MainPanel -> {
            Scaffold(
                floatingActionButton = {
                    AnimatedVisibility(state !is SearchViewState.MainPanel.SearchResult) {
                        FloatingActionButton(
                            onClick = {
                                model.openSearchPanel()
                            }
                        ) {
                            Icon(Icons.Default.Search, null)
                        }
                    }
                }
            ) all@{
                AnimatedContent(state) { state ->
                    when (state) {
                        is SearchViewState.MainPanel.EmptySearch -> {
                            val histories by state.history.collectAsState(listOf())
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Text("搜索")
                                        },
                                        navigationIcon = {
                                            val nav = LocalNavigator.currentOrThrow
                                            IconButton(
                                                onClick = {
                                                    nav.pop()
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                                            }
                                        }
                                    )
                                }
                            ) {
                                if (histories.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(it),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("暂无历史记录")
                                            Text("点击搜索按钮进行一次搜索吧！")
                                        }

                                    }
                                    return@Scaffold
                                }
                                LazyColumn(Modifier.padding(it)) {
                                    items(histories) {
                                        HistoryItem(
                                            onHistoryDelete = {
                                                model.deleteHistory(it)
                                            },
                                            onHistoryClicked = {
                                                model.openSearchPanel(
                                                    sort = it.initialSort,
                                                    target = it.initialTarget,
                                                    keyword = it.keyword
                                                )
                                            },
                                            item = it
                                        )
                                    }
                                }
                            }
                        }

                        is SearchViewState.MainPanel.SearchResult -> {
                            val data = remember(state) {
                                buildList<Pair<String, @Composable () -> Unit>> {
                                    state.illustRepo?.let {
                                        add("插画" to {
                                            IllustFetchScreen(it)
                                        })
                                    }
                                    state.novelRepo?.let {
                                        add("小说" to {
                                            NovelFetchScreen(it)
                                        })
                                    }
                                    state.authorRepo?.let {
                                        add("用户" to {
                                            AuthorFetchScreen(it)
                                        })
                                    }
                                }
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
                                                    model.openSearchPanel(
                                                        sort = state.sort,
                                                        target = state.target,
                                                        keyword = state.keyword
                                                    )
                                                }
                                            ) {
                                                Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                                            }
                                        }
                                    )
                                }
                            ) {
                                TabContainer(
                                    state = tab,
                                    tab = data.map { pair -> pair.first },
                                    modifier = Modifier.padding(it)
                                ) { index ->
                                    data[index].second.invoke()
                                }
                            }
                        }
                    }
                }
            }
        }

        is SearchViewState.SearchPanel -> {
            val keyword by state.keyword.collectAsState()
            val text by state.text.collectAsState()
            val sortState by state.sort.collectAsState()
            val targetState by state.target.collectAsState()


            val chipState = rememberChipTextFieldState(chips = keyword.map { Chip(it) })
            LaunchedEffect(chipState.chips) {
                val target = chipState.chips.map { it.text }
                if (target.size == keyword.size && (target zip keyword).all { it.first == it.second }) {
                    return@LaunchedEffect
                }
                val keyWord = state.keyword as MutableStateFlow<List<String>>
                keyWord.tryEmit(target)
            }

            LaunchedEffect(keyword) {
                chipState.chips = keyword.map { Chip(it) }
            }

            LaunchedEffect(Unit) {
                snapshotFlow { text }
                    .debounce(1.seconds).distinctUntilChanged()
                    .collectLatest { msg ->
                        if (msg.isNotBlank()) model.searchTagOrExactSearch(msg)
                    }
            }

            Scaffold(
                topBar = {
                    ChipTextField(
                        state = chipState,
                        value = text,
                        onValueChange = { state.text.tryEmit(it) },
                        onSubmit = { null },
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    model.closeSearchPanel()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    model.startSearch(
                                        keyword,
                                        sort = sortState,
                                        target = targetState
                                    )
                                },
                                enabled = state !is SearchViewState.SearchPanel.RedirectToPage && keyword.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Search, null)
                            }
                        }
                    )
                }
            ) {
                Column(Modifier.padding(it)) {
                    AnimatedContent(state) { state ->
                        when (state) {
                            is SearchViewState.SearchPanel.SettingProperties -> {
                                val hotTags by state.hotTag.collectAsState()


                                SearchPropertiesPanel(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    sort = sortState,
                                    target = targetState,
                                    tag = hotTags,
                                    onSortChange = { model.updateProperties(sort = it) },
                                    onTargetChange = { model.updateProperties(target = it) },
                                    onTagRequestRefresh = { model.refreshHotTag() },
                                    onTagClicked = { t -> model.selectTag(t.tag) }
                                )
                            }

                            is SearchViewState.SearchPanel.Searching -> {
                                Loading()
                            }

                            is SearchViewState.SearchPanel.SearchingFailed -> {
                                ErrorPage(
                                    text = state.msg,
                                    onClick = {
                                        model.searchTagOrExactSearch(text)
                                    }
                                )
                            }

                            is SearchViewState.SearchPanel.SelectTag -> {
                                LazyColumn {
                                    items(state.tags) {
                                        ListItem(
                                            headlineContent = {
                                                Text(it.name)
                                            },
                                            supportingContent = {
                                                Text(it.translatedName ?: "")
                                            },
                                            modifier = Modifier.clickable {
                                                model.selectTag(it)
                                            }
                                        )
                                    }
                                }
                            }

                            is SearchViewState.SearchPanel.RedirectToPage -> {
                                val nav = LocalNavigator.currentOrThrow
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        if (state.illust != null) {
                                            ListItem(
                                                overlineContent = {
                                                    Text("找到插画")
                                                },
                                                leadingContent = {
                                                    ProgressedAsyncImage(
                                                        url = state.illust.contentImages.get()!![0],
                                                        modifier = Modifier.height(144.dp).aspectRatio(
                                                            ratio = state.illust.width / state.illust.height.toFloat()
                                                        )
                                                    )
                                                },
                                                headlineContent = {
                                                    Text(state.illust.title)
                                                },
                                                supportingContent = {
                                                    AuthorCard(
                                                        user = state.illust.user,
                                                    )
                                                },
                                                modifier = Modifier.clickable {
                                                    nav.push(IllustDetailScreen(state.illust))
                                                }
                                            )
                                        }
                                    }

                                    item {
                                        if (state.novel != null) {
                                            ListItem(
                                                overlineContent = {
                                                    Text("找到小说")
                                                },
                                                leadingContent = {
                                                    ProgressedAsyncImage(
                                                        url = state.novel.imageUrls.medium!!,
                                                        modifier = Modifier.height(144.dp).aspectRatio(70 / 144f)
                                                    )
                                                },
                                                headlineContent = {
                                                    Text(state.novel.title)
                                                },
                                                supportingContent = {
                                                    Text(state.novel.caption, maxLines = 3)
                                                },
                                                modifier = Modifier.clickable {
                                                    nav.push(NovelDetailScreen(state.novel.id.toLong()))
                                                }
                                            )
                                        }

                                    }

                                    item {
                                        if (state.user != null) {
                                            val toast = LocalSnackBarHost.current
                                            ListItem(
                                                overlineContent = {
                                                    Text("找到用户")
                                                },
                                                headlineContent = {},
                                                supportingContent = {
                                                    AuthorCard(
                                                        user = state.user.user,
                                                        onFavoriteClick = {
                                                            toast.showSnackbar("请前往详情页面收藏作者！")
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    item {
                                        if (state.illust == null && state.novel == null && state.user == null) {
                                            ErrorPage(
                                                text = "没有搜索结果",
                                            ) {
                                                model.openSearchPanel()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}