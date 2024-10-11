package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.currentPlatform
import top.kagg886.pmf.backend.useWideScreenMode
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.icon.Download
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.route.main.download.DownloadScreen
import top.kagg886.pmf.ui.route.main.download.DownloadScreenModel
import top.kagg886.pmf.ui.route.main.history.HistoryScreen
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

class IllustDetailScreen : Screen, KoinComponent {
    private var id: Long? = null
    private var illust: Illust? = null

    constructor(id: Long) {
        this.id = id
    }

    constructor(illust: Illust) {
        this.illust = illust
    }

    @OptIn(OrbitExperimental::class)
    private fun loadIllust(model: IllustDetailViewModel) {
        if (id != null) {
            model.loadByIllustId(id!!.toLong())
        }
        if (illust != null) {
            model.loadByIllustBean(illust!!)
        }
    }

    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
        val model = nav.koinNavigatorScreenModel<IllustDetailViewModel>()
        val state by model.collectAsState()
        LaunchedEffect(Unit) {
            loadIllust(model)
        }
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
        val nav = LocalNavigator.currentOrThrow
        val model = nav.koinNavigatorScreenModel<IllustDetailViewModel>()
        when (state) {
            IllustDetailViewState.Error -> {
                ErrorPage(text = "加载失败") {
                    loadIllust(model)
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IllustDetail(illust: Illust) {
        val state = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun IllustPreview(illust: Illust) {
        val img by remember(illust) {
            //TODO should be changed to illust.contentImages[it.Large]
            mutableStateOf(
                illust.metaPages.ifEmpty { listOf(illust.imageUrls) }.map { it.large ?: it.content }
            )
        }
        val nav = LocalNavigator.currentOrThrow
        val model = nav.koinNavigatorScreenModel<IllustDetailViewModel>()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(img) {
                ProgressedAsyncImage(
                    url = it,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().aspectRatio(illust.width.toFloat() / illust.height)
                )
                Spacer(Modifier.height(16.dp))
            }
            item {
                AuthorCard(
                    modifier = Modifier.fillMaxWidth(),
                    user = illust.user
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
                            Row(Modifier.size(120.dp,68.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                val downloadModel = koinScreenModel<DownloadScreenModel>()
                                val snack = LocalSnackBarHost.current
                                val scope = rememberCoroutineScope()
                                IconButton(
                                    onClick = {
                                        illust.originImages!!.map {
                                            downloadModel.startDownload(it)
                                        }
                                        scope.launch {
                                            val result = snack.showSnackbar(
                                                object :SnackbarVisuals {
                                                    override val actionLabel: String?
                                                        get() = "是"
                                                    override val duration: SnackbarDuration
                                                        get() = SnackbarDuration.Long
                                                    override val message: String
                                                        get() = "下载任务已经开始，是否跳转到下载页？"
                                                    override val withDismissAction: Boolean
                                                        get() = true

                                                }
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                nav.push(DownloadScreen())
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Download, null)
                                }
                                FavoriteButton(
                                    isFavorite = illust.isBookMarked,
                                    modifier = Modifier.size(30.dp)
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
                                        keyWords = tag.name
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
    }

    @Composable
    private fun IllustComment(illust: Illust) {
        val model = LocalNavigator.currentOrThrow.koinNavigatorScreenModel<IllustCommentViewModel>()
        LaunchedEffect(illust.id) {
            model.init(illust.id.toLong())
        }

        val state by model.collectAsState()
        IllustCommentContainer(illust, state)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IllustCommentContainer(illust: Illust, state: IllustDetailCommentViewState) {
        when (state) {
            IllustDetailCommentViewState.Loading -> {
                Loading()
            }

            is IllustDetailCommentViewState.Success -> {
                val scroll = state.scrollerState
                val model = LocalNavigator.currentOrThrow.koinNavigatorScreenModel<IllustCommentViewModel>()
                val refreshState = rememberPullToRefreshState { true }

                val host = LocalSnackBarHost.current
                model.collectSideEffect {
                    when (it) {
                        is IllustDetailCommentSideEffect.Toast -> {
                            host.showSnackbar(it.msg)
                        }
                    }
                }

                LaunchedEffect(refreshState.isRefreshing) {
                    if (refreshState.isRefreshing) {
                        model.init(id = illust.id.toLong(), true).join()
                        refreshState.endRefresh()
                    }
                }

                Column {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(refreshState.nestedScrollConnection)
                    ) {
                        val scope = rememberCoroutineScope()
                        if (state.comments.isEmpty()) {
                            ErrorPage(text = "页面为空") {
                                scope.launch {
                                    model.init(illust.id.toLong())
                                }
                            }
                            return@Box
                        }
                        LazyColumn(state = state.scrollerState) {
                            items(state.comments) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth().padding(5.dp)
                                ) {
                                    var showReplies by remember { mutableStateOf(false) }
                                    ListItem(
                                        headlineContent = {
                                            Text(it.user.name, style = MaterialTheme.typography.labelSmall)
                                        },
                                        leadingContent = {
                                            ProgressedAsyncImage(
                                                url = it.user.profileImageUrls.content,
                                                modifier = Modifier.size(35.dp)
                                            )
                                        },
                                        trailingContent = {
                                            if (it.hasReplies) {
                                                IconButton(
                                                    onClick = {
                                                        showReplies = !showReplies
                                                    }
                                                ) {
                                                    Icon(Icons.Default.MoreVert, null)
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Box(modifier = Modifier.padding(5.dp)) {
                                        it.stamp?.let {
                                            ProgressedAsyncImage(
                                                url = it.url,
                                                modifier = Modifier.size(100.dp)
                                            )
                                            return@OutlinedCard
                                        }
                                        Text(it.comment)
                                    }

                                    AnimatedVisibility(
                                        visible = showReplies,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                        modifier = Modifier.padding(5.dp)
                                    ) {
                                        var page by remember {
                                            mutableStateOf(1)
                                        }
                                        val commentReplyModel = remember(it.id, page) {
                                            IllustCommentReplyViewModel(it.id, page)
                                        }
                                        val replyState by commentReplyModel.collectAsState()

                                        when (replyState) {
                                            is IllustCommentReplyState.LoadSuccess -> {
                                                Column(modifier = Modifier.padding(start = 15.dp)) {
                                                    val replies =
                                                        (replyState as IllustCommentReplyState.LoadSuccess).data
                                                    if (replies.isEmpty()) {
                                                        Box(
                                                            Modifier.fillMaxWidth().height(48.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("无回复")
                                                        }
                                                    }
                                                    for (comment in replies) {
                                                        ListItem(
                                                            headlineContent = {
                                                                Text(
                                                                    comment.user.name,
                                                                    style = MaterialTheme.typography.labelSmall
                                                                )
                                                            },
                                                            leadingContent = {
                                                                ProgressedAsyncImage(
                                                                    url = comment.user.profileImageUrls.content,
                                                                    modifier = Modifier.size(25.dp)
                                                                )
                                                            },
                                                        )
                                                        Box(modifier = Modifier.padding(5.dp)) a@{
                                                            it.stamp?.let {
                                                                ProgressedAsyncImage(
                                                                    url = it.url,
                                                                    modifier = Modifier.size(50.dp)
                                                                )
                                                                return@a
                                                            }
                                                            Text(
                                                                it.comment,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceAround
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                if (page >= 2) {
                                                                    page--
                                                                }

                                                            }
                                                        ) {
                                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                if (replies.isNotEmpty()) {
                                                                    page++
                                                                }
                                                            }
                                                        ) {
                                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                                                        }
                                                    }
                                                }
                                            }

                                            IllustCommentReplyState.Loading -> {
                                                Box(modifier = Modifier.height(48.dp)) {
                                                    Loading()
                                                }
                                            }
                                        }
                                    }

                                }
                            }

                            item {
                                LaunchedEffect(Unit) {
                                    if (!state.noMoreData) {
                                        model.loadMore()
                                    }
                                }
                                if (!state.noMoreData) {
                                    Loading()
                                    return@item
                                }
                                Text(
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "没有更多了"
                                )
                            }
                        }
                        PullToRefreshContainer(
                            state = refreshState,
                            modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)
                        )

                        this@Column.AnimatedVisibility(
                            visible = scroll.canScrollBackward,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                            enter = slideInVertically { it / 2 } + fadeIn(),
                            exit = slideOutVertically { it / 2 } + fadeOut()
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        scroll.animateScrollToItem(0)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, null)
                            }
                        }
                    }
                    var text by remember {
                        mutableStateOf("")
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                        },
                        modifier = Modifier.fillMaxWidth().padding(5.dp),
                        label = {
                            Text("评论")
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    model.sendComment(text)
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, null)
                            }
                        }
                    )
                }

            }
        }
    }
}

