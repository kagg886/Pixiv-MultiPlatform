package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinNavigatorScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.rememberRingProgressPainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.parser.NovelContentBlockType.*
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.route.main.search.SearchScreen
import top.kagg886.pmf.ui.route.main.search.SearchTab
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import top.kagg886.pmf.util.splitBy

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
                        val scope = rememberCoroutineScope()
                        IconButton(onClick = {
                            scope.launch {
                                drawer.open()
                            }
                        }) {
                            Icon(Icons.Filled.MoreVert, null)
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
    @OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ProgressedAsyncImage(
                                            url = state.novel.imageUrls.content,
                                            contentScale = ContentScale.FillHeight,
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .height(256.dp).padding(top = 16.dp)
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
                val refreshState = rememberPullToRefreshState { true }

                model.collectSideEffect {
                    when (it) {
                        is NovelDetailCommentSideEffect.Toast -> {
                            detailModel.intent {
                                postSideEffect(NovelDetailSideEffect.Toast(it.msg))
                            }
                        }
                    }
                }

                LaunchedEffect(refreshState.isRefreshing) {
                    if (refreshState.isRefreshing) {
                        model.init(id = novel.id.toLong(), true).join()
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
                                    model.init(novel.id.toLong())
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
                val content = remember {
                    state.content.data.splitBy {
                        it.novelContentBlockType == NEW_PAGE
                    }
                }
                val images = remember {
                    runCatching {
                        state.content.images
                    }.getOrElse { emptyMap() }
                }
                val lazy = rememberLazyListState()
                val scope = rememberCoroutineScope()
                SelectionContainer {
                    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
                        itemsIndexed(content) { index, item ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                HorizontalDivider(Modifier.weight(1f))
                                Text("Page ${index + 1}")
                                HorizontalDivider(Modifier.weight(1f))
                            }
                            val content1 = remember {
                                item.splitBy(true) { it.novelContentBlockType.blocking }
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                for (i in content1) {
                                    if (i.size == 1) {
                                        val it = i[0]
                                        when (it.novelContentBlockType) {
                                            UPLOAD_IMAGE -> {
                                                ProgressedAsyncImage(
                                                    url = images[it.value]!!.urls["480mw"],
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                        .sizeIn(maxWidth = 480.dp)
                                                )
                                            }

                                            PIXIV_IMAGE -> {
                                                val url by remember {
                                                    with(i[0].value!!.split("-")) {
                                                        val page = if (this.size == 2) this[1].toInt() else 0
                                                        model.getIllustLink(this[0].toLong(), page)
                                                    }
                                                }
                                                ProgressedAsyncImage(
                                                    url = url,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            TITLE -> {
                                                Text(it.value!!)
                                            }

                                            JUMP_PAGE -> {
                                                TextButton(
                                                    onClick = {
                                                        scope.launch {
                                                            lazy.animateScrollToItem(it.value!!.toInt())
                                                        }
                                                    }, modifier = Modifier.align(Alignment.CenterHorizontally)
                                                ) {
                                                    Text("跳转到第${it.value!!}页")
                                                }
                                            }

                                            PLAIN -> {
                                                Text(it.value!!)
                                            }

                                            JUMP_URI -> {
                                                val uri = LocalUriHandler.current
                                                ClickableText(
                                                    text = buildAnnotatedString {
                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = Color.Blue.copy(alpha = 0.6f),
                                                                textDecoration = TextDecoration.Underline
                                                            )
                                                        ) {
                                                            append(it.value!!)
                                                        }
                                                    },
                                                    onClick = { _ ->
                                                        uri.openUri(it.metadata!!)
                                                    }
                                                )
                                            }

                                            NOTATION -> {
                                                Text(it.value!!)
                                            }

                                            else -> error("illegal state: $i")
                                        }
                                        continue
                                    }
                                    val anno = buildAnnotatedString {
                                        withStyle(
                                            style = ParagraphStyle(
                                                lineHeight = 1.5.em,
                                                textIndent = TextIndent(firstLine = 2.em),
                                            )
                                        ) {
                                            for (j in i) {
                                                when (j.novelContentBlockType) {
                                                    PLAIN -> {
                                                        j.value!!.lines().forEach {
                                                            append(it)
                                                            appendLine()
                                                        }
                                                    }

                                                    JUMP_URI -> {
                                                        pushStringAnnotation("link", j.value!!)
                                                        withStyle(
                                                            style = SpanStyle(
                                                                color = Color.Blue.copy(alpha = 0.6f),
                                                                textDecoration = TextDecoration.Underline
                                                            )
                                                        ) {
                                                            append(j.value!!)
                                                        }
                                                        pop()
                                                    }

                                                    NOTATION -> append(j.value!!)
                                                    else -> error("illegal state: $j")
                                                }
                                            }
                                        }
                                    }

                                    val uri = LocalUriHandler.current
                                    ClickableText(
                                        text = anno,
                                        onClick = {
                                            anno.getStringAnnotations(tag = "link", start = it, end = it).firstOrNull()
                                                ?.let { range ->
                                                    uri.openUri(range.item.trim())
                                                }
                                        },
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                    }

                }
            }
        }
    }
}