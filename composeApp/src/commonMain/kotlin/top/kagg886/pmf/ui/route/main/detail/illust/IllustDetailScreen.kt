package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
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
import coil3.compose.AsyncImagePainter.State
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.openBrowser
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
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.ui.util.keyboardScrollerController
import top.kagg886.pmf.ui.util.useWideScreenMode
import top.kagg886.pmf.ui.util.withClickable
import top.kagg886.pmf.util.SerializableWrapper
import top.kagg886.pmf.util.toReadableString
import top.kagg886.pmf.util.wrap

// class IllustDetailScreen(val illust0: Illust) : Screen, KoinComponent {
class IllustDetailScreen(illust: SerializableWrapper<Illust>) : Screen, KoinComponent {

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
                        snack.showSnackbar("无法加载插画：$id")
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

    constructor(illust: Illust) : this(wrap(illust))

    private val illust0 by illust

    override val key: ScreenKey
        get() = "illust_detail_${illust0.id}"

    @Composable
    override fun Content() {
        val model = rememberScreenModel(key) {
            IllustDetailViewModel(illust0)
        }
        val state by model.collectAsState()
        val host = LocalSnackBarHost.current
        model.collectSideEffect {
            when (it) {
                is IllustDetailSideEffect.Toast -> host.showSnackbar(it.msg)
            }
        }
        IllustDetailScreenContent(state, model)
    }

    @Composable
    private fun IllustDetailScreenContent(
        state: IllustDetailViewState,
        model: IllustDetailViewModel,
    ) {
        when (state) {
            IllustDetailViewState.Error -> {
                ErrorPage(text = "加载失败") {
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IllustTopAppBar(illust: Illust) {
        val nav = LocalNavigator.currentOrThrow
        TopAppBar(
            title = { Text(text = "图片详情") },
            navigationIcon = {
                IconButton(onClick = { nav.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            actions = {
                var enabled by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { enabled = true },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Icon(Icons.Default.Menu, null)
                }
                DropdownMenu(
                    expanded = enabled,
                    onDismissRequest = { enabled = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("在浏览器中打开") },
                        onClick = {
                            openBrowser("https://pixiv.net/artworks/${illust.id}")
                            enabled = false
                        },
                    )
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
                IllustTopAppBar(state.illust)
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
        ) {
            Scaffold(
                topBar = {
                    IllustTopAppBar(state.illust)
                },
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(it)) {
                    IllustPreview(state, model)
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
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

            var expand by remember { mutableStateOf(false) }
            val img by remember(illust.hashCode(), expand) {
                mutableStateOf(
                    state.data.let {
                        if (!expand) it.take(3) else it
                    },
                )
            }

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

            LazyColumn(
                state = scroll,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            ) {
                items(img) {
                    Spacer(Modifier.height(16.dp))
                    var ratio by remember { mutableStateOf(illust.width.toFloat() / illust.height) }
                    SubcomposeAsyncImage(
                        model = it,
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
                        when (state) {
                            is State.Success -> SubcomposeAsyncImageContent(
                                modifier = Modifier.clickable {
                                    startIndex = img.indexOf(it)
                                    preview = true
                                },
                            )

                            else -> Box(
                                modifier = Modifier.align(Alignment.Center),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                if (illust.contentImages.size > 3 && !expand) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                expand = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("展开更多", textAlign = TextAlign.Center)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    AuthorCard(
                        modifier = Modifier.fillMaxWidth(),
                        user = illust.user,
                        onFavoritePrivateClick = {
                            model.followUser(true).join()
                        },
                    ) {
                        if (it) {
                            model.followUser().join()
                        } else {
                            model.unFollowUser().join()
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        val clipboard = LocalClipboardManager.current
                        val theme = MaterialTheme.colorScheme
                        SupportListItem(
                            overlineContent = {
                                Text(
                                    text = buildAnnotatedString {
                                        withClickable(theme, illust.id.toString()) {
                                            clipboard.setText(
                                                buildAnnotatedString {
                                                    append(illust.id.toString())
                                                },
                                            )
                                            model.intent {
                                                postSideEffect(IllustDetailSideEffect.Toast("复制pid成功"))
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
                                            clipboard.setText(
                                                buildAnnotatedString {
                                                    append(illust.title)
                                                },
                                            )
                                            model.intent {
                                                postSideEffect(IllustDetailSideEffect.Toast("复制标题成功"))
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
                                                title = { Text("高级收藏设置") },
                                                confirm = { tags, publicity ->
                                                    model.likeIllust(publicity, tags).join()
                                                    betterFavoriteDialog = false
                                                },
                                                cancel = {
                                                    betterFavoriteDialog = false
                                                },
                                            )
                                        }

                                        FavoriteButton(
                                            isFavorite = illust.isBookMarked,
                                            modifier = Modifier.size(30.dp),
                                            onDoubleClick = {
                                                betterFavoriteDialog = true
                                            },
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
                                        Text(illust.totalBookmarks.toString())
                                    }
                                    val downloadModel = koinScreenModel<DownloadScreenModel>()
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                downloadModel.startDownload(illust)
                                            },
                                            enabled = illust.contentImages[IllustImagesType.ORIGIN] != null,
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(Download, null)
                                        }
                                        Text("下载")
                                    }
                                }
                            },
                            supportingContent = {
                                SelectionContainer {
                                    HTMLRichText(
                                        html = illust.caption.ifEmpty { "没有简介" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ListItemDefaults.colors().supportingTextColor,
                                    )
                                }
                            },
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedCard {
                        ListItem(
                            overlineContent = {
                                Text("标签")
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
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedCard {
                        ListItem(
                            overlineContent = {
                                Text("发布日期")
                            },
                            headlineContent = {
                                Text(
                                    illust.createTime.toReadableString(),
                                )
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
