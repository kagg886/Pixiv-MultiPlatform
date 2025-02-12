package top.kagg886.pmf.ui.route.main.detail.author

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlinx.coroutines.launch
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.route.main.detail.author.tabs.*
import top.kagg886.pmf.ui.util.KeyListenerFromGlobalPipe
import top.kagg886.pmf.ui.util.collectAsState
import top.kagg886.pmf.ui.util.collectSideEffect
import kotlin.math.max
import kotlin.math.min

class AuthorScreenWithoutCollapse(override val id: Int) : AuthorScreen(id) {
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
    override fun AuthorContent(state: AuthorScreenState) {
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

                Column {
                    ScrollableTabRow(
                        selectedTabIndex = pager.currentPage,
                        modifier = Modifier.fillMaxWidth(),
                        divider = {}
                    ) {
                        val scope = rememberCoroutineScope()
                        //插画作品列表 小说作品列表 插画收藏列表 小说收藏列表 关注的人
                        val tabList = listOf("个人信息", "插画作品", "小说作品", "插画收藏", "小说收藏", "关注")
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
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier.fillMaxWidth()
                    ) { index ->
                        when (index) {
                            0 -> AuthorProfile(state.user)
                            1 -> AuthorIllust(state.user)
                            2 -> AuthorNovel(state.user)
                            3 -> AuthorIllustBookmark(state.user)
                            4 -> AuthorNovelBookmark(state.user)
                            5 -> AuthorFollow(state.user)
                        }
                    }
                }
            }
        }
    }

}