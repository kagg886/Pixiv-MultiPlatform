package top.kagg886.pmf.ui.route.main.detail.author

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.ImagePreviewer
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.ProgressedAsyncImage
import top.kagg886.pmf.ui.component.collapsable.v3.CollapsableTopAppBarScaffold
import top.kagg886.pmf.ui.component.collapsable.v3.LocalConnectedStateKey
import top.kagg886.pmf.ui.route.main.detail.author.tabs.*
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.KeyListenerFromGlobalPipe
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import kotlin.math.max
import kotlin.math.min

open class AuthorScreen(open val id: Int) : Screen {

    override val key: ScreenKey
        get() = "author_$id"

    @Composable
    override fun Content() {
        val model = rememberScreenModel {
            AuthorScreenModel(id)
        }
        val state by model.collectAsState()

        val host = LocalSnackBarHost.current
        model.collectSideEffect {
            when (it) {
                is AuthorScreenSideEffect.Toast -> host.showSnackbar(it.msg)
            }
        }
        Box(Modifier.fillMaxSize()) {
            AuthorContent(state)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    open fun AuthorContent(state: AuthorScreenState) {
        val model = rememberScreenModel {
            AuthorScreenModel(id)
        }
        when (state) {
            AuthorScreenState.Error -> {
                ErrorPage(text = "加载失败", showBackButton = true) {
                    model.loadUserById(id)
                }
            }

            AuthorScreenState.Loading -> {
                Loading()
            }

            is AuthorScreenState.Success -> {
                val pager = rememberPagerState(initialPage = state.initPage) { 5 }

                KeyListenerFromGlobalPipe {
                    if (it.type != KeyEventType.KeyUp) return@KeyListenerFromGlobalPipe
                    when (it.key) {
                        Key.DirectionRight -> {
                            pager.animateScrollToPage(min(pager.currentPage + 1, pager.pageCount - 1))
                        }

                        Key.DirectionLeft -> {
                            pager.animateScrollToPage(max(pager.currentPage - 1, 0))
                        }
                    }
                }


                var infoDialog by remember {
                    mutableStateOf(false)
                }

                if (infoDialog) {
                    AlertDialog(
                        onDismissRequest = { infoDialog = false },
                        confirmButton = {
                            TextButton(onClick = { infoDialog = false }) {
                                Text("关闭")
                            }
                        },
                        title = {
                            Text("个人简介")
                        },
                        text = {
                            Box(Modifier.fillMaxHeight(0.8f)) {
                                AuthorProfile(state.user)
                            }
                        }
                    )
                }

                CollapsableTopAppBarScaffold(
                    background = {
                        Box(modifier = it.fillMaxWidth().height(280.dp)) {
                            var preview by remember { mutableStateOf(false) }
                            if (preview && state.user.profile.backgroundImageUrl != null) {
                                ImagePreviewer(
                                    url = listOf(state.user.profile.backgroundImageUrl!!),
                                    onDismiss = { preview = false }
                                )
                            }

                            ProgressedAsyncImage(
                                url = state.user.profile.backgroundImageUrl,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                                    .clickable(interactionSource = MutableInteractionSource(), indication = null) {
                                        preview = true
                                    }
                            )
                            AuthorCard(
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                user = state.user.user,
                                onCardClick = {
                                    infoDialog = true
                                },
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
                    },
                    navigationIcon = {
                        val nav = LocalNavigator.currentOrThrow
                        IconButton(
                            onClick = {
                                nav.pop()
                            },
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    title = {
                        Text(state.user.user.name)
                    }
                ) {
                    Column(it) {
                        ScrollableTabRow(
                            selectedTabIndex = pager.currentPage,
                            modifier = Modifier.fillMaxWidth(),
                            divider = {}
                        ) {
                            val scope = rememberCoroutineScope()
                            //插画作品列表 小说作品列表 插画收藏列表 小说收藏列表 关注的人
                            val tabList = listOf("插画作品", "小说作品", "插画收藏", "小说收藏", "关注")
                            tabList.forEachIndexed { index, s ->
                                Tab(
                                    selected = pager.currentPage == index,
                                    modifier = Modifier.height(48.dp),
                                    onClick = {
                                        scope.launch {
                                            pager.animateScrollToPage(index)
                                        }
                                    }) {
                                    Text(s)
                                }
                            }
                        }
                        //修复电脑端滚轮
                        CompositionLocalProvider(LocalConnectedStateKey provides this@CollapsableTopAppBarScaffold.connectedScrollState) {
                            HorizontalPager(
                                state = pager,
                                modifier = Modifier.fillMaxWidth()
                            ) { index->
                                when (index) {
                                    0 -> AuthorIllust(state.user)
                                    1 -> AuthorNovel(state.user)
                                    2 -> AuthorIllustBookmark(state.user)
                                    3 -> AuthorNovelBookmark(state.user)
                                    4 -> AuthorFollow(state.user)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}