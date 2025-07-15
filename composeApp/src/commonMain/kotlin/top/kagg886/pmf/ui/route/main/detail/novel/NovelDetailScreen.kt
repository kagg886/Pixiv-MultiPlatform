package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.toUri
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.SeriesInfo
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.advanced_bookmark_settings
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.copy_novel_title_success
import top.kagg886.pmf.copy_pid
import top.kagg886.pmf.create_time
import top.kagg886.pmf.export_to_epub
import top.kagg886.pmf.find_similar_novel
import top.kagg886.pmf.next_page
import top.kagg886.pmf.no_description_novel
import top.kagg886.pmf.novel_comments
import top.kagg886.pmf.novel_detail
import top.kagg886.pmf.novel_intro
import top.kagg886.pmf.openBrowser
import top.kagg886.pmf.open_in_browser
import top.kagg886.pmf.previous_page
import top.kagg886.pmf.series_belong
import top.kagg886.pmf.tags
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.FavoriteButton
import top.kagg886.pmf.ui.component.FavoriteState
import top.kagg886.pmf.ui.component.ImagePreviewer
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.SupportRTLModalNavigationDrawer
import top.kagg886.pmf.ui.component.TabContainer
import top.kagg886.pmf.ui.component.collapsable.v3.connectedScroll
import top.kagg886.pmf.ui.component.collapsable.v3.nestedScrollWorkaround
import top.kagg886.pmf.ui.component.collapsable.v3.rememberConnectedScrollState
import top.kagg886.pmf.ui.component.dialog.TagFavoriteDialog
import top.kagg886.pmf.ui.component.icon.View
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.search.v2.SearchResultScreen
import top.kagg886.pmf.ui.route.main.series.novel.NovelSeriesScreen
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.CommentPanel
import top.kagg886.pmf.ui.util.HTMLRichText
import top.kagg886.pmf.ui.util.KeyListenerFromGlobalPipe
import top.kagg886.pmf.ui.util.RichText
import top.kagg886.pmf.ui.util.keyboardScrollerController
import top.kagg886.pmf.ui.util.withClickable
import top.kagg886.pmf.util.SerializableWrapper
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.setText
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.util.toReadableString
import top.kagg886.pmf.util.wrap

class NovelDetailScreen(private val id: Long, seriesInfo: SerializableWrapper<SeriesInfo>) :
    Screen {
    override val key: ScreenKey
        get() = "novel_detail_$id"

    constructor(id: Long, seriesInfo: SeriesInfo) : this(id, wrap(seriesInfo))

    private val seriesInfo by seriesInfo

    @OptIn(InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        val model = rememberScreenModel("novel_detail_$id") {
            NovelDetailViewModel(id, seriesInfo)
        }

        val ctx = LocalPlatformContext.current
        val state by model.collectAsState()
        LaunchedEffect(Unit) {
            if (state !is NovelDetailViewState.Success) {
                model.reload(ctx)
            }
        }

        val snack = LocalSnackBarHost.current
        val nav = LocalNavigator.currentOrThrow
        model.collectSideEffect {
            when (it) {
                is NovelDetailSideEffect.Toast -> {
                    snack.showSnackbar(it.msg)
                }

                is NovelDetailSideEffect.NavigateToOtherNovel -> {
                    nav.push(NovelDetailScreen(it.id, it.seriesInfo))
                }
            }
        }

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
            drawerState = drawer,
        ) {
            NovelDetailContent(model = model, state = state, modifier = Modifier.fillMaxSize()) {
                scope.launch {
                    drawer.open()
                }
            }
        }
    }

    private class PageScreenModel : ScreenModel {
        val page: MutableState<Int> = mutableIntStateOf(0)
    }

    @Composable
    private fun NovelPreviewContent(model: NovelDetailViewModel, state: NovelDetailViewState) {
        val coil = LocalPlatformContext.current
        when (state) {
            is NovelDetailViewState.Error -> ErrorPage(text = state.cause) {
                model.reload(coil)
            }

            is NovelDetailViewState.Loading -> {
                val text by state.text.collectAsState()
                Loading(text = text)
            }

            is NovelDetailViewState.Success -> {
                val nav = LocalNavigator.currentOrThrow
                val page = rememberScreenModel {
                    PageScreenModel()
                }
                TabContainer(
                    state = page.page,
                    tab = listOf(
                        stringResource(Res.string.novel_intro),
                        stringResource(Res.string.novel_comments, state.novel.totalComments),
                    ),
                ) {
                    when (it) {
                        0 -> {
                            val theme = MaterialTheme.colorScheme
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    var preview by remember { mutableStateOf(false) }
                                    if (preview) {
                                        ImagePreviewer(
                                            onDismiss = { preview = false },
                                            data = listOf(state.novel.imageUrls.contentLarge.toUri()),
                                            startIndex = page.page.value,
                                        )
                                    }
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        AsyncImage(
                                            model = state.novel.imageUrls.content,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                                .height(256.dp).padding(top = 16.dp)
                                                .clickable { preview = true },
                                            contentScale = ContentScale.FillHeight,
                                            contentDescription = null,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = buildAnnotatedString {
                                                withClickable(
                                                    theme,
                                                    state.novel.title,
                                                ) {
                                                    model.intent {
                                                        postSideEffect(
                                                            NovelDetailSideEffect.Toast(
                                                                getString(Res.string.copy_novel_title_success),
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                                .padding(horizontal = 8.dp),
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        AuthorCard(
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                                .fillMaxWidth().padding(horizontal = 8.dp),
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
                                            },
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
                                                SelectionContainer {
                                                    HTMLRichText(
                                                        html = state.novel.caption.ifEmpty {
                                                            stringResource(
                                                                Res.string.no_description_novel,
                                                            )
                                                        },
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }
                                item {
                                    Row(
                                        Modifier.fillMaxSize()
                                            .padding(horizontal = 64.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = View,
                                                contentDescription = null,
                                                modifier = Modifier.size(30.dp),
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
                                                    title = { Text(stringResource(Res.string.advanced_bookmark_settings)) },
                                                    confirm = { tags, publicity ->
                                                        model.likeNovel(publicity, tags).join()
                                                        betterFavoriteDialog = false
                                                    },
                                                    cancel = {
                                                        betterFavoriteDialog = false
                                                    },
                                                )
                                            }
                                            FavoriteButton(
                                                isFavorite = state.novel.isBookmarked,
                                                modifier = Modifier.size(30.dp),
                                                onDoubleClick = {
                                                    betterFavoriteDialog = true
                                                },
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
                                    OutlinedCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                                        ListItem(
                                            overlineContent = {
                                                Text("pid")
                                            },
                                            headlineContent = {
                                                val clip = LocalClipboard.current
                                                Text(
                                                    buildAnnotatedString {
                                                        withClickable(
                                                            theme,
                                                            state.novel.id.toString(),
                                                        ) {
                                                            model.intent {
                                                                clip.setText(
                                                                    state.novel.id.toString(),
                                                                )
                                                                postSideEffect(
                                                                    NovelDetailSideEffect.Toast(
                                                                        getString(Res.string.copy_pid),
                                                                    ),
                                                                )
                                                            }
                                                        }
                                                    },
                                                )
                                            },
                                        )
                                    }
                                }
                                item {
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedCard(Modifier.padding(horizontal = 8.dp)) {
                                        ListItem(
                                            headlineContent = {
                                                Text(stringResource(Res.string.tags))
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
                                                                    SearchResultScreen(
                                                                        keyword = listOf(tag.name),
                                                                        sort = SearchSort.DATE_DESC,
                                                                        target = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
                                                                    ),
                                                                )
                                                            },
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }

                                if (!state.novel.series.isNull) {
                                    item {
                                        Spacer(Modifier.height(16.dp))
                                        OutlinedCard(Modifier.padding(horizontal = 8.dp)) {
                                            ListItem(
                                                headlineContent = {
                                                    Text(stringResource(Res.string.series_belong))
                                                },
                                                supportingContent = {
                                                    Text(state.novel.series.title)
                                                },
                                                modifier = Modifier.clickable {
                                                    nav.push(
                                                        NovelSeriesScreen(
                                                            state.novel.series.id!!.toInt(),
                                                        ),
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedCard(Modifier.padding(horizontal = 8.dp)) {
                                        ListItem(
                                            headlineContent = {
                                                Text(stringResource(Res.string.create_time))
                                            },
                                            supportingContent = {
                                                Text(state.novel.createDate.toReadableString())
                                            },
                                        )
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedCard(Modifier.padding(horizontal = 8.dp)) {
                                        ListItem(
                                            headlineContent = {
                                                Text(stringResource(Res.string.find_similar_novel))
                                            },
                                            modifier = Modifier.clickable {
                                                nav.push(NovelSimilarScreen(state.novel.id.toLong()))
                                            },
                                        )
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }

                        1 -> {
                            NovelComment(state.novel)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NovelComment(novel: Novel) {
        val model = rememberScreenModel("novel_comment_${novel.id}") {
            NovelCommentViewModel(id)
        }
        CommentPanel(model, Modifier.fillMaxSize())
    }

    @Composable
    private fun NovelDetailTopAppBar(
        model: NovelDetailViewModel,
        modifier: Modifier = Modifier,
        onDrawerOpen: () -> Unit = {},
    ) {
        val nav = LocalNavigator.currentOrThrow
        TopAppBar(
            modifier = modifier,
            title = {
                Text(stringResource(Res.string.novel_detail))
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        nav.pop()
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            actions = {
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
                            onDismissRequest = { expanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.export_to_epub)) },
                                onClick = {
                                    model.exportToEpub()
                                    expanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.open_in_browser)) },
                                onClick = {
                                    openBrowser("https://www.pixiv.net/novel/show.php?id=$id")
                                    expanded = false
                                },
                            )
                        }
                    }

                    IconButton(onClick = onDrawerOpen) {
                        Icon(Icons.Default.Edit, null)
                    }
                }
            },
        )
    }

    @Composable
    private fun NovelDetailContent(
        model: NovelDetailViewModel,
        state: NovelDetailViewState,
        modifier: Modifier = Modifier,
        onDrawerOpen: () -> Unit = {},
    ) {
        val ctx = LocalPlatformContext.current
        when (state) {
            is NovelDetailViewState.Error -> {
                Column(modifier) {
                    NovelDetailTopAppBar(model, onDrawerOpen = onDrawerOpen)
                    ErrorPage(Modifier.weight(1f), text = state.cause) {
                        model.reload(ctx)
                    }
                }
            }

            is NovelDetailViewState.Loading -> {
                val text by state.text.collectAsState()

                Column(modifier) {
                    NovelDetailTopAppBar(model, onDrawerOpen = onDrawerOpen)
                    Loading(Modifier.weight(1f), text)
                }
            }

            is NovelDetailViewState.Success -> {
                val scroll = rememberScrollState()
                val connect =
                    rememberConnectedScrollState(immediatelyShowTopBarWhenFingerPullDown = true)

                Column(modifier) {
                    // TopAppBar 使用 connectedScroll 来实现联动滚动
                    // 当向上滚动时会向上移动并减少高度，最终完全隐藏
                    NovelDetailTopAppBar(
                        model = model,
                        modifier = Modifier.connectedScroll(connect),
                        onDrawerOpen = onDrawerOpen,
                    )

                    // 内容区域，应用 nestedScroll 来处理滚动事件
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(connect.nestedScrollConnection)
                            .nestedScrollWorkaround(scroll, connect),
                    ) {
                        val controller = remember {
                            keyboardScrollerController(scroll) {
                                scroll.viewportSize.toFloat()
                            }
                        }

                        KeyListenerFromGlobalPipe(controller)

                        Column(Modifier.verticalScroll(scroll)) {
                            RichText(
                                state = state.nodeMap,
                                modifier = Modifier
                                    .padding(horizontal = 15.dp),
                            )

                            if (AppConfig.enableFetchSeries) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        64.dp,
                                        alignment = Alignment.CenterHorizontally
                                    )
                                ) {
                                    TextButton(
                                        onClick = {
                                            model.navigatePreviousPage()
                                        },
                                    ) {
                                        Text(stringResource(Res.string.previous_page))
                                    }
                                    TextButton(
                                        onClick = {
                                            model.navigateNextPage()
                                        },
                                    ) {
                                        Text(stringResource(Res.string.next_page))
                                    }
                                }
                            }
                        }

                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scroll),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 5.dp)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}
