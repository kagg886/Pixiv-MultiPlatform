package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.jetbrains.compose.resources.ExperimentalResourceApi
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.dialog.TagFavoriteDialog
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.route.main.search.SearchTab
import top.kagg886.pmf.ui.route.main.series.novel.NovelSeriesScreen
import top.kagg886.pmf.ui.util.*
import top.kagg886.pmf.util.toReadableString
import top.kagg886.pmf.view


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
            is NovelDetailViewState.Error -> ErrorPage(text = state.cause) {
                model.reload()
            }

            NovelDetailViewState.Loading -> Loading()
            is NovelDetailViewState.Success -> {
                val nav = LocalNavigator.currentOrThrow
                val page = rememberScreenModel {
                    PageScreenModel()
                }
                TabContainer(
                    state = page.page,
                    tab = listOf("简介", "评论:${state.novel.totalComments}")
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
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = state.novel.title,
                                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 8.dp),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        AuthorCard(
                                            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().padding(horizontal = 8.dp),
                                            state.novel.user,
                                            onFavoriteClick = {
                                                if (it) {
                                                    model.followUser().join()
                                                    return@AuthorCard
                                                }
                                                model.unFollowUser().join()
                                            },
                                            onFavoritePrivateClick = {
                                                model.followUser(true).join()
                                            }
                                        )
                                    }
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(Modifier.height(16.dp))
                                }
                                item {
                                    OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                                        ListItem(
                                            headlineContent = {
                                                Text(state.novel.caption)
                                            }
                                        )
                                    }
                                }
                                item {
                                    Row(
                                        Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                painter =  painterResource(Res.drawable.view),
                                                contentDescription = null,
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Text(state.novel.totalView.toString())
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            var betterFavoriteDialog by remember {
                                                mutableStateOf(false)
                                            }
                                            if (betterFavoriteDialog) {
                                                TagFavoriteDialog(
                                                    tags = state.novel.tags,
                                                    title = { Text("高级收藏设置") },
                                                    confirm = { tags, publicity ->
                                                        model.likeNovel(publicity, tags).join()
                                                        betterFavoriteDialog = false
                                                    },
                                                    cancel = {
                                                        betterFavoriteDialog = false
                                                    }
                                                )
                                            }
                                            FavoriteButton(
                                                isFavorite = state.novel.isBookmarked,
                                                modifier = Modifier.size(30.dp),
                                                onDoubleClick = {
                                                    betterFavoriteDialog = true
                                                }
                                            ) {
                                                if (it == FavoriteState.Favorite) {
                                                    model.likeNovel().join()
                                                    return@FavoriteButton
                                                }
                                                if (it == FavoriteState.NotFavorite) {
                                                    model.disLikeNovel().join()
                                                    return@FavoriteButton
                                                }
                                            }
                                            Text(state.novel.totalBookmarks.toString())
                                        }
                                    }
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(Modifier.height(16.dp))
                                }
                                item {
                                    OutlinedCard(Modifier.padding(horizontal = 8.dp)) {
                                        ListItem(
                                            headlineContent = {
                                                Text("标签")
                                            },
                                            supportingContent = {
                                                FlowRow(
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
                                        )
                                    }
                                }

                                if (!state.novel.series.isNull) {
                                    item {
                                        Spacer(Modifier.height(16.dp))
                                        OutlinedCard(Modifier.padding(horizontal = 8.dp)) {
                                            ListItem(
                                                headlineContent = {
                                                    Text("所属系列")
                                                },
                                                supportingContent = {
                                                    Text(state.novel.series.title)
                                                },
                                                modifier = Modifier.clickable {
                                                    nav.push(
                                                        NovelSeriesScreen(
                                                            state.novel.series.id!!.toInt()
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedCard(Modifier.padding(horizontal = 8.dp)) {
                                        ListItem(
                                            headlineContent = {
                                                Text("创建时间")
                                            },
                                            supportingContent = {
                                                Text(state.novel.createDate.toReadableString())
                                            }
                                        )
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(16.dp))
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
            is NovelDetailViewState.Error -> {
                ErrorPage(modifier, text = state.cause) {
                    model.reload()
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