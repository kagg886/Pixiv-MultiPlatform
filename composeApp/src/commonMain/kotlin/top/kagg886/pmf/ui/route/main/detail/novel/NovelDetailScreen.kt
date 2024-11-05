package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.rememberRingProgressPainter
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.route.main.search.SearchTab
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect


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
                        IconButton(onClick = {
                            scope.launch {
                                drawer.open()
                            }
                        }) {
                            Icon(Icons.Default.Edit, null)
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
        val model = LocalNavigator.currentOrThrow.koinNavigatorScreenModel<NovelCommentViewModel>()
        LaunchedEffect(novel.id) {
            model.init(novel.id.toLong())
        }

        val state by model.collectAsState()
        NovelCommentContainer(novel, detailViewModel, state)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NovelCommentContainer(
        novel: Novel,
        detailModel: NovelDetailViewModel,
        state: NovelDetailCommentViewState
    ) {
        when (state) {
            NovelDetailCommentViewState.Loading -> {
                Loading()
            }

            is NovelDetailCommentViewState.Success -> {
                val scroll = state.scrollerState
                val model = LocalNavigator.currentOrThrow.koinNavigatorScreenModel<NovelCommentViewModel>()

                model.collectSideEffect {
                    when (it) {
                        is NovelDetailCommentSideEffect.Toast -> {
                            detailModel.intent {
                                postSideEffect(NovelDetailSideEffect.Toast(it.msg))
                            }
                        }
                    }
                }

                var isRefreshing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Column {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            scope.launch {
                                model.init(id = novel.id.toLong(), true).join()
                            }.invokeOnCompletion {
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        if (state.comments.isEmpty()) {
                            ErrorPage(text = "页面为空") {
                                scope.launch {
                                    model.init(novel.id.toLong())
                                }
                            }
                            return@PullToRefreshBox
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
                                            NovelCommentReplyViewModel(it.id, page)
                                        }
                                        val replyState by commentReplyModel.collectAsState()

                                        when (replyState) {
                                            is NovelCommentReplyState.LoadSuccess -> {
                                                Column(modifier = Modifier.padding(start = 15.dp)) {
                                                    val replies =
                                                        (replyState as NovelCommentReplyState.LoadSuccess).data
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
                                                                val progressPainter = rememberRingProgressPainter()
                                                                val imageState = rememberAsyncImageState()
                                                                ProgressedAsyncImage(
                                                                    url = it.url,
                                                                    modifier = Modifier.size(50.dp)
                                                                        .progressIndicator(imageState, progressPainter)
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

                                            NovelCommentReplyState.Loading -> {
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
                Markdown(
                    content = state.content,
                    colors = markdownColor(),
                    typography = markdownTypography(
                        paragraph = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            textIndent = TextIndent(firstLine = 24.sp, restLine = 0.sp),
                            lineBreak = LineBreak.Heading
                        )
                    ),
                    components = markdownComponents(
                        paragraph = {
                            MarkdownParagraph(
                                content = it.content,
                                node = it.node,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    ),
                    imageTransformer = object : ImageTransformer {
                        @Composable
                        override fun transform(link: String): ImageData {
                            val painter = rememberAsyncImagePainter(
                                request = ComposableImageRequest(link)
                            )
                            var show by remember { mutableStateOf(false) }
                            if (show) {
                                ImagePreviewer(
                                    onDismiss = { show = false },
                                    url = listOf(link)
                                )
                            }
                            return ImageData(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.clickable {
                                    show = true
                                }
                            )
                        }
                    },
                    modifier = modifier.padding(15.dp).verticalScroll(rememberScrollState())
                )
            }
        }
    }
}