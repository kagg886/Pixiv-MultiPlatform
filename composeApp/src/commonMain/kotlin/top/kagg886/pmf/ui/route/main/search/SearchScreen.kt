package top.kagg886.pmf.ui.route.main.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchSort.*
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.SearchTarget.*
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen
import top.kagg886.pmf.ui.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

enum class SearchTab(val display: String) {
    ILLUST("插画"),
    NOVEL("小说"),
    AUTHOR("作者")
}

class SearchScreen(
    private val initialSort: SearchSort = DATE_DESC,
    private val initialTarget: SearchTarget = PARTIAL_MATCH_FOR_TAGS,
    private val initialKeyWords: String = "",
    val tab: SearchTab = SearchTab.ILLUST
) : Screen {
    private class PageScreenModel(
        val page: MutableState<Int> = mutableIntStateOf(0)
    ) : ScreenModel


    private var illustDirty = -1
    private var novelDirty = -1
    private var authorDirty = -1


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


        var active by rememberSaveable { mutableStateOf(false) }
        val padding by animateDpAsState(if (active) 0.dp else 16.dp)

        var sort by rememberSaveable { mutableStateOf(initialSort) }
        var target by rememberSaveable { mutableStateOf(initialTarget) }
        var keyWords by rememberSaveable { mutableStateOf(initialKeyWords) }

        var searchWords by rememberSaveable { mutableStateOf(initialKeyWords) }
        LaunchedEffect(Unit) {
            snapshotFlow { keyWords }.debounce(1.seconds).distinctUntilChanged().collectLatest {
                if (keyWords.isEmpty()) {
                    searchWords = ""
                }
                model.searchTag(keyWords.split(" ").last())
            }
        }
        fun startSearch() {
            if (keyWords.isEmpty()) {
                return
            }

            with(Random(System.currentTimeMillis())) {
                illustDirty = nextInt()
                novelDirty = nextInt()
                authorDirty = nextInt()
            }
            searchWords = keyWords
            active = false
            model.saveSearchHistory(sort, target, searchWords, tab)
        }
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = keyWords,
                        onSearch = { startSearch() },
                        expanded = active,
                        onExpandedChange = { active = it },
                        onQueryChange = { keyWords = it },
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
                        placeholder = { Text("Search...") }
                    )
                },
                expanded = active,
                onExpandedChange = {},
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = padding),
            ) {
                when (state) {
                    is SearchViewState.EmptySearch -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = SearchBarDefaults.colors().containerColor
                                )
                            )
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

                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = SearchBarDefaults.colors().containerColor
                                )
                            )
                            val tags by state.tag.collectAsState(null)
                            ListItem(
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            model.refreshTag()
                                        },
                                        enabled = tags != null
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            null
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text("热门tag")
                                },
                                supportingContent = {
                                    if (tags == null) {
                                        LinearProgressIndicator()
                                        return@ListItem
                                    }
                                    if (tags?.isFailure == true) {
                                        Text("加载失败")
                                        return@ListItem
                                    }
                                    FlowRow {
                                        for (tag in tags?.getOrThrow() ?: emptyList()) {
                                            AssistChip(
                                                onClick = {
                                                    model.saveSearchHistory(
                                                        sort, target, tag.tag.name, SearchTab.ILLUST
                                                    )
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
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = SearchBarDefaults.colors().containerColor
                                )
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

                    is SearchViewState.SearchFailed -> {
                        ErrorPage(text = state.msg) {
                            model.searchTag(keyWords.split(" ").last())
                        }
                    }
                }

            }
            if (searchWords.isNotEmpty()) {
                SearchResultContent(
                    searchWords,
                    target,
                    sort,
                )
                return
            }
            if (state is CanAccessHistory) {
                val list by state.history.collectAsState(initial = listOf())
                if (list.isNotEmpty()) {
                    LazyColumn {
                        items(list) {
                            ListItem(
                                overlineContent = {
                                    Text(it.initialKeyWords)
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            model.deleteSearchHistory(it)
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "")
                                    }
                                },
                                headlineContent = {
                                    FlowRow {
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    when (it.initialSort) {
                                                        DATE_DESC -> "时间倒序"
                                                        DATE_ASC -> "时间正序"
                                                        POPULAR_DESC -> "热门倒序"
                                                    }
                                                )
                                            },
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    when (it.initialTarget) {
                                                        PARTIAL_MATCH_FOR_TAGS -> "部分匹配"
                                                        EXACT_MATCH_FOR_TAGS -> "精确匹配"
                                                        TITLE_AND_CAPTION -> "标题简介"
                                                    }
                                                )
                                            },
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(it.tab.display)
                                            },
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    nav.push(
                                        SearchScreen(
                                            initialSort = it.initialSort,
                                            initialTarget = it.initialTarget,
                                            initialKeyWords = it.initialKeyWords,
                                            tab = it.tab
                                        )
                                    )
                                }
                            )
                        }
                    }
                    return
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无历史记录")
                    Text("点击搜索框进行一次搜索吧！")
                }
            }
        }
    }

    @Composable
    private fun SearchResultContent(
        searchWords: String,
        target: SearchTarget,
        sort: SearchSort,
    ) {
        if (searchWords.toIntOrNull() != null) {
            val model = rememberScreenModel<IdSearchViewModel>(tag = "search_exactly_${searchWords}") {
                IdSearchViewModel(searchWords.toLong())
            }
            val state by model.collectAsState()
            ExactlySearchResultContent(model, state)
            return
        }
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
                        rememberScreenModel(tag = "search_result_illust_${searchWords}_${target}_${sort}_${illustDirty}") {
                            SearchResultIllustModel(
                                word = searchWords,
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
                        rememberScreenModel(tag = "search_result_novel_${searchWords}_${target}_${sort}_${novelDirty}") {
                            SearchResultNovelModel(
                                word = searchWords,
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

                2 -> {
                    val resultModel =
                        rememberScreenModel(tag = "search_result_author_${searchWords}_${authorDirty}") {
                            SearchResultUserModel(user = searchWords)
                        }
                    resultModel.collectSideEffect { effect ->
                        when (effect) {
                            is AuthorFetchSideEffect.Toast -> {
                                snackbarHostState.showSnackbar(effect.msg)
                            }
                        }
                    }
                    AuthorFetchScreen(resultModel)
                }
            }
        }
    }

    @Composable
    private fun ExactlySearchResultContent(model: IdSearchViewModel, state: IdSearchViewState) {
        when (state) {
            is IdSearchViewState.LoadSuccess -> {
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
                                model.search()
                            }
                        }

                    }
                }
            }
            IdSearchViewState.Loading -> {
                Loading()
            }
        }
    }
}