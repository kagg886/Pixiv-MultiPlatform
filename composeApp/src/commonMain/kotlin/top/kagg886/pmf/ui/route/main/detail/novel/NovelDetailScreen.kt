package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.route.main.search.SearchTab
import top.kagg886.pmf.ui.util.*


class NovelDetailScreen(private val id: Long) : Screen {
    override val key: ScreenKey
        get() = "novel_detail_$id"

    @OptIn(ExperimentalMaterial3Api::class, InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        val model = rememberScreenModel("novel_detail_$id") {
            NovelDetailViewModel(id)
        }
        val snack = LocalSnackBarHost.current
        model.collectSideEffect {
            when (it) {
                is NovelDetailSideEffect.Toast -> {
                    snack.showSnackbar(it.msg)
                }
            }
        }

        val state by model.collectAsState()

        val drawer = rememberDrawerState(DrawerValue.Closed)

        val scope = rememberCoroutineScope()
        BackHandler(drawer.isOpen) {
            scope.launch {
                drawer.close()
            }
        }

        SupportRTLModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    NovelPreviewContent(model, state)
                }
            },
            rtlLayout = true,
            drawerState = drawer
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(title = {
                        Text("小说详情")
                    }, navigationIcon = {
                        IconButton(onClick = {
                            nav.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }, actions = {
                        Row {
                            Column {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        expanded = true
                                    },
                                ) {
                                    Icon(Icons.Default.Menu, null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("导出为epub") },
                                        onClick = {
                                            model.exportToEpub()
                                        }
                                    )
                                }

                            }

                            IconButton(onClick = {
                                scope.launch {
                                    drawer.open()
                                }
                            }) {
                                Icon(Icons.Default.Edit, null)
                            }
                        }

                    })
                }
            ) {
                NovelDetailContent(model, state, Modifier.padding(it))
            }
        }

    }

    private class PageScreenModel : ScreenModel {
        val page: MutableState<Int> = mutableIntStateOf(0)
    }

    @Composable
    @OptIn(ExperimentalLayoutApi::class)
    private fun NovelPreviewContent(model: NovelDetailViewModel, state: NovelDetailViewState) {
        when (state) {
            NovelDetailViewState.Error -> ErrorPage(text = "加载失败惹~！") {
                model.loadByNovelId(id)
            }

            NovelDetailViewState.Loading -> Loading()
            is NovelDetailViewState.Success -> {
                val nav = LocalNavigator.currentOrThrow
                val page = rememberScreenModel {
                    PageScreenModel()
                }
                TabContainer(
                    state = page.page,
                    tab = listOf("简介", "评论")
                ) {
                    when (it) {
                        0 -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    var preview by remember { mutableStateOf(false) }
                                    if (preview) {
                                        ImagePreviewer(
                                            onDismiss = { preview = false },
                                            url = listOf(state.novel.imageUrls.contentLarge),
                                            startIndex = page.page.value
                                        )
                                    }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ProgressedAsyncImage(
                                            url = state.novel.imageUrls.content,
                                            contentScale = ContentScale.FillHeight,
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .height(256.dp).padding(top = 16.dp)
                                                .clickable {
                                                    preview = true
                                                }
                                        )
                                        Text(
                                            text = state.novel.title,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                                .padding(top = 16.dp),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = state.novel.caption,
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                                .align(Alignment.CenterHorizontally)
                                                .padding(top = 16.dp),
                                        )
                                        ListItem(
                                            headlineContent = {
                                                Text(state.novel.user.name)
                                            },
                                            supportingContent = {
                                                Text(
                                                    state.novel.user.comment?.lines()?.first()
                                                        ?.takeIf { it.isNotEmpty() }
                                                        ?: "没有简介")
                                            },
                                            leadingContent = {
                                                ProgressedAsyncImage(
                                                    url = state.novel.user.profileImageUrls.content,
                                                    modifier = Modifier.size(35.dp)
                                                )
                                            },
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .align(Alignment.CenterHorizontally)
                                                .fillMaxWidth(0.8f)
                                                .clickable {
                                                    nav.push(AuthorScreen(state.novel.user.id))
                                                }
                                        )
                                    }
                                }
                                item {
                                    HorizontalDivider()
                                }
                                item {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        for (tag in state.novel.tags) {
                                            AssistChip(
                                                label = {
                                                    Text(text = tag.name)
                                                },
                                                onClick = {
                                                    nav.push(
                                                        SearchScreen(
                                                            initialKeyWords = tag.name,
                                                            tab = SearchTab.NOVEL
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            NovelComment(state.novel, model)
                        }
                    }

                }
            }
        }
    }

    @Composable
    private fun NovelComment(novel: Novel, detailViewModel: NovelDetailViewModel) {
        val model = rememberScreenModel("novel_comment_${novel.id}") {
            NovelCommentViewModel(id)
        }
        CommentPanel(model, Modifier.fillMaxSize())
    }

    @Composable
    private fun NovelDetailContent(model: NovelDetailViewModel, state: NovelDetailViewState, modifier: Modifier) {
        when (state) {
            NovelDetailViewState.Error -> {
                ErrorPage(modifier, text = "加载失败惹~！") {
                    model.loadByNovelId(id)
                }
            }

            NovelDetailViewState.Loading -> {
                Loading(modifier)
            }


            is NovelDetailViewState.Success -> {
                Box(modifier.fillMaxWidth()) {
                    val scroll = rememberScrollState()
                    RichText(
                        state = state.nodeMap.toSortedMap().map { it.value },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp).verticalScroll(scroll)
                    )

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scroll),
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).fillMaxHeight()
                    )
                }
            }
        }
    }
}