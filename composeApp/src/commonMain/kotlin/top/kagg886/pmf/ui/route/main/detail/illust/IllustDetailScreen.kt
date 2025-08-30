package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import coil3.EventListener
import coil3.compose.AsyncImagePainter.State
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.backend.Platform
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.bookmark_extra_options
import top.kagg886.pmf.cant_load_illust
import top.kagg886.pmf.copy_pid
import top.kagg886.pmf.copy_title_success
import top.kagg886.pmf.download
import top.kagg886.pmf.error
import top.kagg886.pmf.expand_more
import top.kagg886.pmf.find_similar_illust
import top.kagg886.pmf.image_details
import top.kagg886.pmf.no_description
import top.kagg886.pmf.openBrowser
import top.kagg886.pmf.open_in_browser
import top.kagg886.pmf.publish_date
import top.kagg886.pmf.show_original_image
import top.kagg886.pmf.tags
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.FavoriteButton
import top.kagg886.pmf.ui.component.FavoriteState
import top.kagg886.pmf.ui.component.ImagePreviewer
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.SupportListItem
import top.kagg886.pmf.ui.component.SupportRTLModalNavigationDrawer
import top.kagg886.pmf.ui.component.dialog.TagFavoriteDialog
import top.kagg886.pmf.ui.component.icon.Download
import top.kagg886.pmf.ui.component.icon.View
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.download.DownloadScreenModel
import top.kagg886.pmf.ui.route.main.search.v2.SearchResultScreen
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.CommentPanel
import top.kagg886.pmf.ui.util.HTMLRichText
import top.kagg886.pmf.ui.util.KeyListenerFromGlobalPipe
import top.kagg886.pmf.ui.util.RollingNumber
import top.kagg886.pmf.ui.util.illustRouter
import top.kagg886.pmf.ui.util.keyboardScrollerController
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.ui.util.withClickable
import top.kagg886.pmf.util.SerializableWrapper
import top.kagg886.pmf.util.getString
import top.kagg886.pmf.util.logger
import top.kagg886.pmf.util.setText
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.util.toReadableString
import top.kagg886.pmf.util.wrap

class IllustDetailScreen(illust: SerializableWrapper<Illust>, todos: SerializableWrapper<List<Illust>>) :
    Screen,
    KoinComponent {

    class PreFetch(private val id: Long) : Screen, KoinComponent {
        private val client = PixivConfig.newAccountFromConfig()

        @Composable
        override fun Content() {
            val nav = LocalNavigator.currentOrThrow
            val snack = LocalSnackBarHost.current
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                scope.launch {
                    val illust = kotlin.runCatching {
                        client.getIllustDetail(id)
                    }
                    if (illust.isFailure) {
                        snack.showSnackbar(getString(Res.string.cant_load_illust, id))
                        return@launch
                    }
                    nav.replace(IllustDetailScreen(illust.getOrThrow()))
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Loading()
                IconButton(
                    onClick = {
                        scope.cancel()
                        nav.pop()
                    },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
        }
    }

    constructor(illust: Illust, todos: List<Illust> = listOf(illust)) : this(wrap(illust), wrap(todos))

    private val current by illust
    private val todos by todos

    override val key: ScreenKey
        get() = "illust_detail_${current.id}"

    init {
        if (this.todos.size > 1) {
            logger.i("Experimental Horizonal Illust: total is ${this.todos.size} current is ${this.todos.indexOf(current)}")
        }
    }

    @Composable
    override fun Content() = BoxWithConstraints(Modifier.fillMaxSize()) {
        HorizontalPager(state = rememberPagerState(initialPage = todos.indexOf(current)) { todos.size }) { index ->
            val illust = todos[index]
            val model = rememberScreenModel(illust.hashCode().toString()) {
                IllustDetailViewModel(illust)
            }
            val state by model.collectAsState()
            val host = LocalSnackBarHost.current
            model.collectSideEffect {
                when (it) {
                    is IllustDetailSideEffect.Toast -> host.showSnackbar(it.msg)
                }
            }
            Box(Modifier.width(maxWidth).height(maxHeight)) {
                IllustDetailScreenContent(state, model)
            }
        }
    }

    @Composable
    private fun IllustDetailScreenContent(
        state: IllustDetailViewState,
        model: IllustDetailViewModel,
    ) {
        when (state) {
            IllustDetailViewState.Error -> {
                ErrorPage(text = stringResource(Res.string.error)) {
                    model.load()
                }
            }

            is IllustDetailViewState.Loading -> {
                val text by state.data.collectAsState()
                Loading(text = text)
            }

            is IllustDetailViewState.Success -> {
                if (useWideScreenMode) {
                    WideScreenIllustDetail(state, model)
                    return
                }
                IllustDetail(state, model)
            }
        }
    }

    @Composable
    private fun IllustTopAppBar(illust: Illust, onCommentPanelBtnClick: () -> Unit = {}, onOriginImageRequest: () -> Unit = {}) {
        val nav = LocalNavigator.currentOrThrow
        TopAppBar(
            title = { Text(text = stringResource(Res.string.image_details)) },
            navigationIcon = {
                IconButton(onClick = { nav.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            actions = {
                var enabled by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { enabled = true },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(Icons.Default.Menu, null)
                }
                DropdownMenu(
                    expanded = enabled,
                    onDismissRequest = { enabled = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.open_in_browser)) },
                        onClick = {
                            openBrowser("https://pixiv.net/artworks/${illust.id}")
                            enabled = false
                        },
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.show_original_image)) },
                        onClick = onOriginImageRequest,
                    )
                }
                if (currentPlatform !is Platform.Desktop) {
                    IconButton(
                        onClick = onCommentPanelBtnClick,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Icon(Icons.Default.Edit, null)
                    }
                }
            },
        )
    }

    @Composable
    private fun WideScreenIllustDetail(
        state: IllustDetailViewState.Success,
        model: IllustDetailViewModel,
    ) {
        Scaffold(
            topBar = {
                IllustTopAppBar(
                    illust = state.illust,
                    onCommentPanelBtnClick = {},
                    onOriginImageRequest = {
                        model.toggleOrigin()
                    }
                )
            },
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(it)) {
                Box(Modifier.fillMaxWidth(0.7f).fillMaxHeight()) {
                    IllustPreview(state, model)
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    IllustComment(state.illust)
                }
            }
        }
    }

    @OptIn(InternalVoyagerApi::class, InternalVoyagerApi::class)
    @Composable
    private fun IllustDetail(
        state: IllustDetailViewState.Success,
        model: IllustDetailViewModel,
    ) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        BackHandler(drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }
        SupportRTLModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    IllustComment(state.illust)
                }
            },
            rtlLayout = true,
            drawerState = drawerState,
            // 滑动展开comment模式且comment panel open时，需要启用手势滑动关闭
            gesturesEnabled = AppConfig.illustDetailOpenFor == AppConfig.DetailSlideOpenFor.OpenComment || drawerState.isOpen,
        ) {
            Scaffold(
                topBar = {
                    IllustTopAppBar(
                        illust = state.illust,
                        onCommentPanelBtnClick = {
                            scope.launch {
                                if (drawerState.isOpen) {
                                    drawerState.close()
                                } else {
                                    drawerState.open()
                                }
                            }
                        },
                        onOriginImageRequest = {
                            model.toggleOrigin()
                        }
                    )
                },
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(it)) {
                    IllustPreview(state, model)
                }
            }
        }
    }

    @Composable
    private fun IllustPreview(state: IllustDetailViewState.Success, model: IllustDetailViewModel) {
        val illust = state.illust
        Box(modifier = Modifier.fillMaxSize()) {
            val scroll = rememberLazyListState()

            val controller = remember {
                keyboardScrollerController(scroll) {
                    scroll.layoutInfo.viewportSize.height.toFloat()
                }
            }

            KeyListenerFromGlobalPipe(controller)

            var preview by remember { mutableStateOf(false) }
            var startIndex by remember { mutableStateOf(0) }
            if (preview) {
                ImagePreviewer(
                    onDismiss = { preview = false },
                    data = state.data, // preview should be show all
                    modifier = Modifier.fillMaxSize(),
                    startIndex = startIndex,
                )
            }

            var expand by remember { mutableStateOf(AppConfig.illustDetailsShowAll) }
            val img by remember(state.data.hashCode(), expand) {
                mutableStateOf(state.data.let { if (!expand) it.take(3) else it })
            }
            LazyColumn(
                state = scroll,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(img, key = { i, _ -> i }) { i, uri ->
                    var ratio by remember { mutableStateOf(illust.width.toFloat() / illust.height) }
                    SubcomposeAsyncImage(
                        model = uri,
                        modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
                        onState = { state: State ->
                            if (state is State.Success) {
                                val image = state.result.image
                                ratio = image.width.toFloat() / image.height.toFloat()
                            }
                        },
                        contentDescription = null,
                    ) {
                        val state by painter.state.collectAsState()
                        when (val s = state) {
                            is State.Success -> SubcomposeAsyncImageContent(
                                modifier = Modifier.clickable {
                                    startIndex = i
                                    preview = true
                                },
                            )

                            is State.Loading -> Box(
                                modifier = Modifier.align(Alignment.Center),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }

                            is State.Error -> ErrorPage(
                                modifier = Modifier.align(Alignment.Center),
                                text = s.result.throwable.message ?: "Unknown Error",
                                onClick = { model.load() },
                            )

                            else -> Unit
                        }
                    }
                }
                if (illust.contentImages.size > 3 && !expand) {
                    item(key = "expand") {
                        TextButton(
                            onClick = { expand = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(Res.string.expand_more),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                item(key = "author") {
                    AuthorCard(
                        modifier = Modifier.fillMaxWidth(),
                        user = illust.user,
                        onFavoritePrivateClick = { model.followUser(true).join() },
                    ) {
                        if (it) {
                            model.followUser().join()
                        } else {
                            model.unFollowUser().join()
                        }
                    }
                }
                item(key = "info") {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        val clipboard = LocalClipboard.current
                        val theme = MaterialTheme.colorScheme
                        SupportListItem(
                            overlineContent = {
                                Text(
                                    text = buildAnnotatedString {
                                        withClickable(theme, illust.id.toString()) {
                                            model.intent {
                                                clipboard.setText(
                                                    illust.id.toString(),
                                                )
                                                postSideEffect(
                                                    IllustDetailSideEffect.Toast(
                                                        getString(Res.string.copy_pid),
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = buildAnnotatedString {
                                        withClickable(theme, illust.title) {
                                            model.intent {
                                                clipboard.setText(illust.title)
                                                postSideEffect(
                                                    IllustDetailSideEffect.Toast(
                                                        getString(Res.string.copy_title_success),
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                )
                            },
                            trailingContent = {
                                Row(
                                    Modifier.size(120.dp, 68.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = View,
                                            contentDescription = null,
                                            modifier = Modifier.size(30.dp),
                                        )
                                        Text(illust.totalView.toString())
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        var betterFavoriteDialog by remember {
                                            mutableStateOf(false)
                                        }
                                        if (betterFavoriteDialog) {
                                            TagFavoriteDialog(
                                                tags = illust.tags,
                                                title = { Text(stringResource(Res.string.bookmark_extra_options)) },
                                                confirm = { tags, publicity ->
                                                    model.likeIllust(publicity, tags).join()
                                                    betterFavoriteDialog = false
                                                },
                                                cancel = {
                                                    betterFavoriteDialog = false
                                                },
                                            )
                                        }

                                        val illust by illustRouter.collectLatest(illust)
                                        FavoriteButton(
                                            isFavorite = illust.isBookMarked,
                                            modifier = Modifier.size(30.dp),
                                            onDoubleClick = { betterFavoriteDialog = true },
                                        ) {
                                            if (it == FavoriteState.Favorite) {
                                                model.likeIllust().join()
                                                return@FavoriteButton
                                            }
                                            if (it == FavoriteState.NotFavorite) {
                                                model.disLikeIllust().join()
                                                return@FavoriteButton
                                            }
                                        }
                                        RollingNumber(illust.totalBookmarks)
                                    }
                                    val downloadModel = koinScreenModel<DownloadScreenModel>()
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                downloadModel.startIllustDownload(illust)
                                            },
                                            enabled = illust.contentImages[IllustImagesType.ORIGIN] != null,
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(Download, null)
                                        }
                                        Text(stringResource(Res.string.download))
                                    }
                                }
                            },
                            supportingContent = {
                                SelectionContainer {
                                    HTMLRichText(
                                        html = illust.caption.ifEmpty { stringResource(Res.string.no_description) },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ListItemDefaults.colors().supportingTextColor,
                                    )
                                }
                            },
                        )
                    }
                }
                item(key = "tags") {
                    OutlinedCard {
                        ListItem(
                            overlineContent = {
                                Text(stringResource(Res.string.tags))
                            },
                            headlineContent = {
                                FlowRow {
                                    val nav = LocalNavigator.currentOrThrow
                                    for (tag in illust.tags) {
                                        AssistChip(
                                            modifier = Modifier.padding(4.dp),
                                            onClick = {
                                                nav.push(
                                                    SearchResultScreen(
                                                        keyword = listOf(tag.name),
                                                        sort = SearchSort.DATE_DESC,
                                                        target = SearchTarget.PARTIAL_MATCH_FOR_TAGS,
                                                    ),
                                                )
                                            },
                                            label = {
                                                Column {
                                                    Text(
                                                        tag.name,
                                                        style = MaterialTheme.typography.labelMedium,
                                                    )
                                                    tag.translatedName?.let {
                                                        Text(
                                                            it,
                                                            style = MaterialTheme.typography.labelSmall,
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
                item(key = "publish_date") {
                    OutlinedCard {
                        ListItem(
                            overlineContent = {
                                Text(stringResource(Res.string.publish_date))
                            },
                            headlineContent = {
                                Text(
                                    illust.createTime.toReadableString(),
                                )
                            },
                        )
                    }
                }

                item(key = "similar") {
                    OutlinedCard {
                        val nav = LocalNavigator.currentOrThrow
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(Res.string.find_similar_illust),
                                )
                            },
                            modifier = Modifier.clickable {
                                nav.push(IllustSimilarScreen(illust.id.toLong()))
                            },
                        )
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scroll),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).fillMaxHeight(),
            )
        }
    }

    @Composable
    private fun IllustComment(illust: Illust) {
        val model = rememberScreenModel(tag = "illust_comment_${illust.id}") {
            IllustCommentViewModel(illust.id.toLong())
        }

        CommentPanel(
            model = model,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
