package top.kagg886.pmf.ui.route.main.search.v2

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.dokar.chiptextfield.Chip
import com.dokar.chiptextfield.ChipTextFieldState
import com.dokar.chiptextfield.m3.ChipTextField
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import top.kagg886.pixko.Tag
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen
import top.kagg886.pmf.ui.route.main.search.v2.components.SearchPropertiesPanel
import top.kagg886.pmf.ui.route.main.series.novel.NovelSeriesScreen
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import kotlin.time.Duration.Companion.seconds

class SearchPanelScreen(
    private val sort: SearchSort = SearchSort.DATE_DESC,
    private val target: SearchTarget = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
    private val keyword: List<String> = listOf(),
    private val initialText: String = ""
) : Screen {
    @OptIn(InternalVoyagerApi::class, FlowPreview::class)
    @Composable
    override fun Content() {
        val model = rememberScreenModel { SearchPanelViewModel(sort, target, keyword, initialText) }
        val state by model.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }

        model.collectSideEffect { sideEffect ->
            when (sideEffect) {
                is SearchPanelSideEffect.Toast -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
            }
        }

        BackHandler(true) {
            navigator.pop()
        }

        val chipState = remember(state.keyword) {
            ChipTextFieldState(chips = state.keyword.map { Chip(it) })
        }

        LaunchedEffect(chipState.chips) {
            val target = chipState.chips.map { it.text }
            if (target.size == state.keyword.size && (target zip state.keyword).all { it.first == it.second }) {
                return@LaunchedEffect
            }
            model.updateKeywords(target)
        }

        Scaffold(
            topBar = {
                when (state.target) {
                    SearchTarget.EXACT_MATCH_FOR_TAGS, SearchTarget.PARTIAL_MATCH_FOR_TAGS -> {
                        LaunchedEffect(Unit) {
                            snapshotFlow { state.text }.debounce(0.5.seconds)
                                .distinctUntilChanged()
                                .collectLatest { msg ->
                                    if (msg.isNotBlank()) {
                                        model.searchTagOrExactSearch(msg)
                                    }
                                }
                        }
                        ChipTextField(
                            state = chipState,
                            value = state.text,
                            onValueChange = { model.updateText(it) },
                            onSubmit = {
                                model.selectTag(Tag(it))
                                model.updateText("")
                                Chip(it)
                            },
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        navigator.pop()
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (state.keyword.isNotEmpty()) {
                                            navigator.push(
                                                SearchResultScreen(
                                                    state.keyword,
                                                    state.sort,
                                                    state.target
                                                )
                                            )
                                        }
                                    },
                                    enabled = state.keyword.isNotEmpty() && state.panelState !is SearchPanelState.RedirectToPage
                                ) {
                                    Icon(Icons.Default.Search, null)
                                }
                            }
                        )
                    }
                    else -> {
                        LaunchedEffect(Unit) {
                            model.updateKeywords(listOf())
                            model.updateText("")
                        }
                        TextField(
                            value = state.text,
                            modifier = Modifier.fillMaxWidth(),
                            onValueChange = { model.updateText(it) },
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        navigator.pop()
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (state.text.isNotEmpty()) {
                                            navigator.replace(
                                                SearchResultScreen(
                                                    listOf(state.text),
                                                    state.sort,
                                                    state.target
                                                )
                                            )
                                        }
                                    },
                                    enabled = state.text.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.Search, null)
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(Modifier.padding(paddingValues)) {
                AnimatedContent(state.panelState) { currentState ->
                    when (currentState) {
                        is SearchPanelState.SettingProperties -> {
                            SearchPropertiesPanel(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                sort = state.sort,
                                target = state.target,
                                tag = state.hotTag,
                                onSortChange = { model.updateSort(it) },
                                onTargetChange = { model.updateTarget(it) },
                                onTagRequestRefresh = { model.refreshHotTag() },
                                onTagClicked = { t -> model.selectTag(t.tag) }
                            )
                        }

                        is SearchPanelState.Searching -> {
                            Loading()
                        }

                        is SearchPanelState.SearchingFailed -> {
                            ErrorPage(
                                text = currentState.msg,
                                onClick = {
                                    model.searchTagOrExactSearch(state.text)
                                }
                            )
                        }

                        is SearchPanelState.SelectTag -> {
                            LazyColumn {
                                items(currentState.tags) {
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

                        is SearchPanelState.RedirectToPage -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    if (currentState.illust != null) {
                                        ListItem(
                                            overlineContent = {
                                                Text("找到插画")
                                            },
                                            leadingContent = {
                                                ProgressedAsyncImage(
                                                    url = currentState.illust.contentImages.get()!![0],
                                                    modifier = Modifier.height(144.dp).aspectRatio(
                                                        ratio = currentState.illust.width / currentState.illust.height.toFloat()
                                                    )
                                                )
                                            },
                                            headlineContent = {
                                                Text(currentState.illust.title)
                                            },
                                            supportingContent = {
                                                AuthorCard(
                                                    user = currentState.illust.user,
                                                )
                                            },
                                            modifier = Modifier.clickable {
                                                navigator.push(IllustDetailScreen(currentState.illust))
                                            }
                                        )
                                    }
                                }

                                item {
                                    if (currentState.novel != null) {
                                        ListItem(
                                            overlineContent = {
                                                Text("找到小说")
                                            },
                                            leadingContent = {
                                                ProgressedAsyncImage(
                                                    url = currentState.novel.imageUrls.medium!!,
                                                    modifier = Modifier.height(144.dp).aspectRatio(70 / 144f)
                                                )
                                            },
                                            headlineContent = {
                                                Text(currentState.novel.title)
                                            },
                                            supportingContent = {
                                                Text(currentState.novel.caption, maxLines = 3)
                                            },
                                            modifier = Modifier.clickable {
                                                navigator.push(NovelDetailScreen(currentState.novel.id.toLong()))
                                            }
                                        )
                                    }
                                }

                                item {
                                    if (currentState.series != null) {
                                        ListItem(
                                            overlineContent = {
                                                Text("找到小说系列")
                                            },
                                            headlineContent = {
                                                Text(currentState.series.novelSeriesDetail.title)
                                            },
                                            modifier = Modifier.clickable {
                                                navigator.push(NovelSeriesScreen(currentState.series.novelSeriesDetail.id))
                                            }
                                        )
                                    }
                                }

                                item {
                                    if (currentState.user != null) {
                                        val toast = LocalSnackBarHost.current
                                        ListItem(
                                            overlineContent = {
                                                Text("找到用户")
                                            },
                                            headlineContent = {},
                                            supportingContent = {
                                                AuthorCard(
                                                    user = currentState.user.user,
                                                    onFavoriteClick = {
                                                        toast.showSnackbar("请前往详情页面收藏作者！")
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }

                                item {
                                    if (currentState.illust == null && currentState.novel == null &&
                                        currentState.user == null && currentState.series == null) {
                                        ErrorPage(
                                            text = "没有搜索结果",
                                        ) {}
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
