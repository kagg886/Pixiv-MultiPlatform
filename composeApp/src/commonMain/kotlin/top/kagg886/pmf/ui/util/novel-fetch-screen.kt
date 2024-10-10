package top.kagg886.pmf.ui.util

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen

@Composable
fun NovelFetchScreen(model: NovelFetchViewModel) {
    val state by model.collectAsState()
    NovelFetchContent0(state, model)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NovelFetchContent0(state: NovelFetchViewState, model: NovelFetchViewModel) {
    val nav = LocalNavigator.currentOrThrow
    when (state) {
        NovelFetchViewState.Loading -> {
            Loading()
        }

        is NovelFetchViewState.ShowNovelList -> {
            val scroll = state.scrollerState

            val refreshState = rememberPullToRefreshState { true }

            LaunchedEffect(refreshState.isRefreshing) {
                if (refreshState.isRefreshing) {
                    model.initNovel(true).join()
                    refreshState.endRefresh()
                }
            }

            Box(modifier = Modifier.fillMaxSize().nestedScroll(refreshState.nestedScrollConnection)) {
                val scope = rememberCoroutineScope()
                if (state.novels.isEmpty()) {
                    ErrorPage(text = "页面为空") {
                        scope.launch {
                            model.initNovel()
                        }
                    }
                    return@Box
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = scroll
                ) {
                    items(state.novels, key = { it.id }) {
                        Column {
                            ListItem(
                                headlineContent = {
                                    Text(it.title)
                                },
                                supportingContent = {
                                    Text(it.tags.take(20).joinToString(", ") { it.name }, minLines = 3, maxLines = 3)
                                },
                                leadingContent = {
                                    ProgressedAsyncImage(
                                        url = it.imageUrls.content,
                                        contentScale = ContentScale.FillHeight,
                                        modifier = Modifier.widthIn(max = 70.dp).height(90.dp).padding(8.dp)
                                    )
                                },
                                trailingContent = {
                                    FavoriteButton(
                                        isFavorite = it.isBookmarked,
                                        onModify = { target ->
                                            if (target == FavoriteState.Favorite) {
                                                model.likeNovel(it).join()
                                            } else {
                                                model.disLikeNovel(it).join()
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.padding(5.dp).clickable {
                                    nav.push(NovelDetailScreen(it.id.toLong()))
                                }
                            )
                        }
                    }
                    item {
                        LaunchedEffect(Unit) {
                            if (!state.noMoreData) {
                                model.loadMoreNovels()
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
                AnimatedVisibility(
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
        }
    }
}
