package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ComposableImageRequest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.dialog.TagFavoriteDialog
import top.kagg886.pmf.ui.component.icon.Download
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.download.DownloadScreenModel
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.util.*
import top.kagg886.pmf.util.SerializableWrapper
import top.kagg886.pmf.util.wrap

//class IllustDetailScreen(val illust0: Illust) : Screen, KoinComponent {
class IllustDetailScreen(illust: SerializableWrapper<Illust>) : Screen, KoinComponent {

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
        IllustDetailScreenContent(state)
    }

    @Composable
    private fun IllustDetailScreenContent(state: IllustDetailViewState) {
        val model = rememberScreenModel<IllustDetailViewModel>(key) {
            error("not provided")
        }
        when (state) {
            IllustDetailViewState.Error -> {
                ErrorPage(text = "加载失败") {
                    model.load()
                }
            }

            IllustDetailViewState.Loading -> {
                Loading()
            }

            is IllustDetailViewState.Success -> {
                if (currentPlatform.useWideScreenMode) {
                    WideScreenIllustDetail(state.illust)
                    return
                }
                IllustDetail(state.illust)
            }
        }

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun WideScreenIllustDetail(illust: Illust) {
        val nav = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "图片详情") },
                    navigationIcon = {
                        IconButton(onClick = { nav.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            }
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(it)) {
                val padding = PaddingValues(horizontal = 25.dp)
                Box(Modifier.fillMaxWidth(0.7f).fillMaxHeight().padding(padding)) {
                    IllustPreview(illust)
                }
                Box(Modifier.weight(1f).fillMaxHeight().padding(padding)) {
                    IllustComment(illust)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, InternalVoyagerApi::class, InternalVoyagerApi::class)
    @Composable
    private fun IllustDetail(illust: Illust) {
        val state = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        BackHandler(state.isOpen) {
            scope.launch {
                state.close()
            }
        }
        SupportRTLModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        IllustComment(illust)
                    }
                }
            },
            rtlLayout = true,
            drawerState = state
        ) {
            val nav = LocalNavigator.currentOrThrow
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = "图片详情") },
                        navigationIcon = {
                            IconButton(onClick = { nav.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                scope.launch {
                                    state.open()
                                }
                            }) {
                                Icon(Icons.Filled.Edit, null)
                            }
                        }
                    )
                }
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(it)) {
                    IllustPreview(illust)
                }
            }
        }

    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalResourceApi::class)
    @Composable
    private fun IllustPreview(illust: Illust) {
        val img by remember(illust.hashCode()) {
            mutableStateOf(
                illust.contentImages[IllustImagesType.LARGE, IllustImagesType.MEDIUM]!!
            )
        }
        val nav = LocalNavigator.currentOrThrow
        val model = rememberScreenModel<IllustDetailViewModel>(key) {
            error("not provided")
        }

        var preview by remember { mutableStateOf(false) }
        var startIndex by remember { mutableStateOf(0) }

        if (preview) {
            ImagePreviewer(
                onDismiss = { preview = false },
                url = img,
                modifier = Modifier.fillMaxSize(),
                startIndex = startIndex
            )
        }
        val needExpand by remember {
            derivedStateOf { img.size > 3 }
        }
        var expand by remember {
            mutableStateOf(false)
        }

        val show = remember(expand) {
            if (expand) img else img.take(3)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val scroll = rememberLazyListState()
            LazyColumn(state = scroll, modifier = Modifier.padding(end = 8.dp)) {
                items(show) {
                    ProgressedAsyncImage(
                        url = it,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                            .aspectRatio(illust.width.toFloat() / illust.height)
                            .clickable {
                                startIndex = img.indexOf(it)
                                preview = true
                            }
                    )
                    Spacer(Modifier.height(16.dp))
                }
                if (needExpand && !expand) {
                    item {
                        TextButton(
                            onClick = {
                                expand = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("展开更多", textAlign = TextAlign.Center)
                        }
                    }
                }
                item {
                    AuthorCard(
                        modifier = Modifier.fillMaxWidth(),
                        user = illust.user,
                        onFavoritePrivateClick = {
                            model.followUser(true).join()
                        }
                    ) {
                        if (it) {
                            model.followUser().join()
                        } else {
                            model.unFollowUser().join()
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            overlineContent = {
                                Text(illust.id.toString(), style = MaterialTheme.typography.labelSmall)
                            },
                            headlineContent = {
                                Text(illust.title)
                            },
                            supportingContent = {
                                Text(illust.caption.ifEmpty { "没有简介" }, style = MaterialTheme.typography.labelLarge)
                            },
                            trailingContent = {
                                Row(
                                    Modifier.size(120.dp, 68.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = rememberAsyncImagePainter(
                                                request = ComposableImageRequest(
                                                    uri = Res.getUri("drawable/view.svg")
                                                )
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(30.dp)
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
                                                }
                                            )
                                        }

                                        FavoriteButton(
                                            isFavorite = illust.isBookMarked,
                                            modifier = Modifier.size(30.dp),
                                            onDoubleClick = {
                                                betterFavoriteDialog = true
                                            }
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
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Download, null)
                                        }
                                        Text("下载")
                                    }
                                }
                            }
                        )
                    }
                }
                item {
                    FlowRow {
                        for (tag in illust.tags) {
                            AssistChip(
                                modifier = Modifier.padding(4.dp),
                                onClick = {
                                    nav.push(
                                        SearchScreen(
                                            initialKeyWords = tag.name
                                        )
                                    )
                                },
                                label = {
                                    Column {
                                        Text(tag.name, style = MaterialTheme.typography.labelMedium)
                                        tag.translatedName?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scroll),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
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
            modifier = Modifier.fillMaxSize()
        )
    }
}

