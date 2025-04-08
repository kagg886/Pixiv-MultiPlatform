package top.kagg886.pmf.ui.route.main.detail.author

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import coil3.compose.AsyncImage
import coil3.toUri
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.closed
import top.kagg886.pmf.load_failed
import top.kagg886.pmf.openBrowser
import top.kagg886.pmf.personal_profile
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.ImagePreviewer
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.collapsable.v3.CollapsableTopAppBarScaffold
import top.kagg886.pmf.ui.component.collapsable.v3.LocalConnectedStateKey
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorFollow
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorIllust
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorIllustBookmark
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorNovel
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorNovelBookmark
import top.kagg886.pmf.ui.route.main.detail.author.tabs.AuthorProfile
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.KeyListenerFromGlobalPipe
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

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

    @Composable
    open fun AuthorContent(state: AuthorScreenState) {
        val model = rememberScreenModel {
            AuthorScreenModel(id)
        }
        when (state) {
            AuthorScreenState.Error -> {
                ErrorPage(text = stringResource(Res.string.load_failed), showBackButton = true) {
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
                                Text(stringResource(Res.string.closed))
                            }
                        },
                        title = {
                            Text(stringResource(Res.string.personal_profile))
                        },
                        text = {
                            Box(Modifier.fillMaxHeight(0.8f)) {
                                AuthorProfile(state.user)
                            }
                        },
                    )
                }

                CollapsableTopAppBarScaffold(
                    background = {
                        Box(modifier = it.fillMaxWidth().height(280.dp)) {
                            var preview by remember { mutableStateOf(false) }
                            if (preview && state.user.profile.backgroundImageUrl != null) {
                                ImagePreviewer(
                                    data = listOf(state.user.profile.backgroundImageUrl!!.toUri()),
                                    onDismiss = { preview = false },
                                )
                            }

                            AsyncImage(
                                model = state.user.profile.backgroundImageUrl,
                                modifier = Modifier.fillMaxSize()
                                    .clickable(
                                        interactionSource = MutableInteractionSource(),
                                        indication = null,
                                        onClick = { preview = true },
                                    ),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                            )
                            AuthorCard(
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                user = state.user.user,
                                onCardClick = {
                                    infoDialog = true
                                },
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
                    actions = {
                        var show by remember {
                            mutableStateOf(false)
                        }
                        DropdownMenu(
                            expanded = show,
                            onDismissRequest = { show = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("在浏览器中打开")
                                },
                                onClick = {
                                    openBrowser(
                                        "https://www.pixiv.net/users/${state.user.user.id}",
                                    )
                                    show = false
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                show = true
                            },
                        ) {
                            Icon(Icons.Default.Menu, null)
                        }
                    },
                    title = {
                        Text(state.user.user.name)
                    },
                ) {
                    Column(it) {
                        ScrollableTabRow(
                            selectedTabIndex = pager.currentPage,
                            modifier = Modifier.fillMaxWidth(),
                            divider = {},
                        ) {
                            val scope = rememberCoroutineScope()
                            // 插画作品列表 小说作品列表 插画收藏列表 小说收藏列表 关注的人
                            val tabList = listOf("插画作品", "小说作品", "插画收藏", "小说收藏", "关注")
                            tabList.forEachIndexed { index, s ->
                                Tab(
                                    selected = pager.currentPage == index,
                                    modifier = Modifier.height(48.dp),
                                    onClick = {
                                        scope.launch {
                                            pager.animateScrollToPage(index)
                                        }
                                    },
                                ) {
                                    Text(s)
                                }
                            }
                        }
                        // 修复电脑端滚轮
                        CompositionLocalProvider(LocalConnectedStateKey provides this@CollapsableTopAppBarScaffold.connectedScrollState) {
                            HorizontalPager(
                                state = pager,
                                modifier = Modifier.fillMaxWidth(),
                            ) { index ->
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
