package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.github.panpf.sketch.fetch.newBase64Uri
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ImageResult
import com.github.panpf.sketch.util.MimeTypeMap
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustImagesType
import top.kagg886.pixko.module.illust.get
import top.kagg886.pixko.module.illust.getIllustDetail
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.openBrowser
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.dialog.TagFavoriteDialog
import top.kagg886.pmf.ui.component.icon.Download
import top.kagg886.pmf.ui.component.icon.View
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.download.DownloadScreenModel
import top.kagg886.pmf.ui.route.main.search.v2.SearchParam
import top.kagg886.pmf.ui.route.main.search.v2.SearchScreen
import top.kagg886.pmf.ui.util.*
import top.kagg886.pmf.util.SerializableWrapper
import top.kagg886.pmf.util.absolutePath
import top.kagg886.pmf.util.toReadableString
import top.kagg886.pmf.util.wrap

//class IllustDetailScreen(val illust0: Illust) : Screen, KoinComponent {
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
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
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

            is IllustDetailViewState.Loading -> {
                val text by state.data.collectAsState()
                Loading(text = text)
            }

            is IllustDetailViewState.Success -> {
                if (useWideScreenMode) {
                    WideScreenIllustDetail(state.illust,state)
                    return
                }
                IllustDetail(state.illust,state)
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
                    modifier = Modifier.padding(horizontal = 8.dp)
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
                        }
                    )
                }
            }
        )
    }

    @Composable
    private fun WideScreenIllustDetail(illust: Illust,illustState: IllustDetailViewState.Success) {
        Scaffold(
            topBar = {
                IllustTopAppBar(illust)
            }
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(it)) {
                Box(Modifier.fillMaxWidth(0.7f).fillMaxHeight()) {
                    when(illustState) {
                        is IllustDetailViewState.Success.GIF -> GIFPreview(illust, illustState)
                        is IllustDetailViewState.Success.Normal -> IllustPreview(illust)
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    IllustComment(illust)
                }
            }
        }
    }

    @OptIn(InternalVoyagerApi::class, InternalVoyagerApi::class)
    @Composable
    private fun IllustDetail(illust: Illust,illustState: IllustDetailViewState.Success) {
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
                    IllustComment(illust)
                }
            },
            rtlLayout = true,
            drawerState = state
        ) {
            Scaffold(
                topBar = {
                    IllustTopAppBar(illust)
                }
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(it)) {
                    when(val s = illustState) {
                        is IllustDetailViewState.Success.GIF -> GIFPreview(illust,s)
                        is IllustDetailViewState.Success.Normal -> IllustPreview(illust)
                    }
                }
            }
        }
    }

    @Composable
    fun GIFPreview(illust: Illust, state: IllustDetailViewState.Success.GIF) {
        Box(Modifier.fillMaxSize()) {
            val scroll = rememberLazyListState()

            val controller = remember {
                keyboardScrollerController(scroll) {
                    scroll.layoutInfo.viewportSize.height.toFloat()
                }
            }

            KeyListenerFromGlobalPipe(controller)

            LazyColumn(state = scroll, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                item {
                    var show by remember {
                        mutableStateOf(false)
                    }
                    if (show) {
                        ImagePreviewer(
                            onDismiss = {show = false},
                            url = listOf(state.data),
                            startIndex = 0,
                        )
                    }
                    ProgressedAsyncImage(
                        url = state.data,
                        modifier = Modifier.fillMaxWidth()
                            .aspectRatio(state.illust.width.toFloat() / state.illust.height.toFloat())
                            .clickable { show = true }
                    )
                }

                previewCommonItem(illust)
            }

        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    fun LazyListScope.previewCommonItem(illust: Illust) {
        item {
            val model = rememberScreenModel<IllustDetailViewModel>(key) {
                error("not provided")
            }
            Spacer(Modifier.height(16.dp))
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
        }
        item {
            val model = rememberScreenModel<IllustDetailViewModel>(key) {
                error("not provided")
            }
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
                                        }
                                    )
                                    model.intent {
                                        postSideEffect(IllustDetailSideEffect.Toast("复制pid成功"))
                                    }
                                }
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    headlineContent = {
                        Text(
                            text = buildAnnotatedString {
                                withClickable(theme, illust.title) {
                                    clipboard.setText(
                                        buildAnnotatedString {
                                            append(illust.title)
                                        }
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = View,
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
                    },
                    supportingContent = {
                        SelectionContainer {
                            HTMLRichText(
                                html = illust.caption.ifEmpty { "没有简介" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = ListItemDefaults.colors().supportingTextColor,
                            )
                        }
                    }
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
                                            SearchScreen(
                                                param = SearchParam.KeyWordSearch(listOf(tag.name))
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
                            illust.createTime.toReadableString()
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun IllustPreview(illust: Illust) {
        Box(modifier = Modifier.fillMaxSize()) {
            val scroll = rememberLazyListState()

            val controller = remember {
                keyboardScrollerController(scroll) {
                    scroll.layoutInfo.viewportSize.height.toFloat()
                }
            }

            KeyListenerFromGlobalPipe(controller)


            var expand by remember {
                mutableStateOf(false)
            }
            val img by remember(illust.hashCode(), expand) {
                mutableStateOf(
                    illust.contentImages[IllustImagesType.LARGE, IllustImagesType.MEDIUM]!!.let {
                        if (!expand) it.take(3) else it
                    }
                )
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


            LazyColumn(state = scroll, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                items(img) {
                    Spacer(Modifier.height(16.dp))
                    val state = rememberAsyncImageState()
                    val haveRatio = remember {
                        derivedStateOf {
                            state.result is ImageResult.Success
                        }
                    }
                    val ratio = remember(haveRatio) {
                        if (state.result is ImageResult.Success) {
                            val info = (state.result as ImageResult.Success).imageInfo
                            return@remember info.width.toFloat() / info.height.toFloat()
                        }
                        illust.width.toFloat() / illust.height
                    }
                    ProgressedAsyncImage(
                        url = it,
                        state = state,
                        modifier = Modifier.fillMaxWidth()
                            .aspectRatio(ratio)
                            .clickable {
                                startIndex = img.indexOf(it)
                                preview = true
                            }
                    )
                }
                if (needExpand && !expand) {
                    item {
                        Spacer(Modifier.height(16.dp))
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

                previewCommonItem(illust)
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scroll),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).fillMaxHeight()
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

