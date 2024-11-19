package top.kagg886.pmf.ui.route.main.detail.author

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import top.kagg886.pmf.ui.component.collapsable.CollapsableColumn
import top.kagg886.pmf.ui.component.collapsable.rememberCollapsableTopBehavior
import top.kagg886.pmf.ui.route.main.detail.author.tabs.*
import top.kagg886.pmf.ui.util.AuthorCard
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect

class AuthorScreen(val id: Int, val isOpenInSideBar: Boolean = false) : Screen {

    override val key: ScreenKey
        get() = "author_$id"

    @Composable
    override fun Content() {
        val nav = LocalNavigator.currentOrThrow
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

            if (!isOpenInSideBar) {
                Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    IconButton(onClick = {
                        nav.pop()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun AuthorContent(state: AuthorScreenState) {
        val model = rememberScreenModel {
            AuthorScreenModel(id)
        }
        when (state) {
            AuthorScreenState.Error -> {
                ErrorPage(text = "加载失败") {
                    model.loadUserById(id)
                }
            }

            AuthorScreenState.Loading -> {
                Loading()
            }

            is AuthorScreenState.Success -> {
                val collapsableBehavior = rememberCollapsableTopBehavior()
                val pager = rememberPagerState(initialPage = state.initPage) { 5 }


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
                            AuthorProfile(state.user)
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(collapsableBehavior.nestedScrollConnection),
                    topBar = {
                        CollapsableColumn(behavior = collapsableBehavior) {
                            Box(modifier = Modifier.fillMaxWidth().height(280.dp).collapse()) {

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
                                    modifier = Modifier.fillMaxSize().clickable { preview = true }
                                )
                                AuthorCard(
                                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                                    user = state.user.user,
                                    onCardClick = {
                                        infoDialog = true
                                    }
                                ) {
                                    if (it) {
                                        model.followUser().join()
                                    } else {
                                        model.unFollowUser().join()
                                    }
                                }
//                                Card(
//                                    colors = with(CardDefaults.cardColors()) {
//                                        copy(containerColor = containerColor.copy(alpha = 0.6f))
//                                    },
//                                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
//                                ) {
//                                    ListItem(
//                                        leadingContent = {
//                                            ProgressedAsyncImage(
//                                                modifier = Modifier.size(64.dp).padding(8.dp),
//                                                url = state.user.user.profileImageUrls.content
//                                            )
//                                        },
//                                        headlineContent = {
//                                            Text(state.user.user.name)
//                                        },
//                                        supportingContent = {
//                                            Text(
//                                                text = state.user.user.comment?.ifBlank { "没有简介" } ?: "没有简介",
//                                                maxLines = 1,
//                                                overflow = TextOverflow.Ellipsis)
//                                        }
//                                    )
//                                }
                            }
                            ScrollableTabRow(
                                selectedTabIndex = pager.currentPage, modifier = Modifier.fillMaxWidth(),
                                divider = {}
                            ) {
                                val scope = rememberCoroutineScope()
                                //插画作品列表 小说作品列表 插画收藏列表 小说收藏列表 关注的人
                                val tabList = listOf("插画作品", "小说作品", "插画收藏", "小说收藏", "关注")
                                tabList.forEachIndexed { index, s ->
                                    Tab(
                                        selected = pager.currentPage == index,
                                        modifier = Modifier.height(36.dp),
                                        onClick = {
                                            scope.launch {
                                                pager.animateScrollToPage(index)
                                            }
                                        }) {
                                        Text(s)
                                    }
                                }
                            }
                        }
                    }
                ) { padding ->
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.fillMaxWidth().padding(padding)
                    ) {
                        when (it) {
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