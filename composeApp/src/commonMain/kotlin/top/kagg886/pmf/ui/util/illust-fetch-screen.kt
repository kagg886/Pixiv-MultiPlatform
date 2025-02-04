package top.kagg886.pmf.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.dialog.TagFavoriteDialog
import top.kagg886.pmf.ui.component.icon.R18
import top.kagg886.pmf.ui.component.icon.R18G
import top.kagg886.pmf.ui.component.icon.Robot
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.detail.illust.IllustDetailScreen

@Composable
fun IllustFetchScreen(model: IllustFetchViewModel) {
    val state by model.collectAsState()
    IllustFetchContent0(state, model)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IllustFetchContent0(state: IllustFetchViewState, model: IllustFetchViewModel) {
    val nav = LocalNavigator.currentOrThrow
    when (state) {
        IllustFetchViewState.Loading -> {
            Loading()
        }

        is IllustFetchViewState.ShowIllustList -> {
            val scroll = state.scrollerState
            val scope = rememberCoroutineScope()
            var isRefresh by remember { mutableStateOf(false) }

            val controller = remember {
                keyboardScrollerController(scroll) {
                    scroll.layoutInfo.viewportSize.height.toFloat()
                }
            }

            KeyListenerFromGlobalPipe(controller)

            PullToRefreshBox(
                isRefreshing = isRefresh,
                onRefresh = {
                    isRefresh = true
                    scope.launch {
                        model.initIllust(true).join()
                    }.invokeOnCompletion {
                        isRefresh = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.illusts.isEmpty()) {
                    ErrorPage(text = "页面为空") {
                        scope.launch {
                            model.initIllust()
                        }
                    }
                    return@PullToRefreshBox
                }
                val columns = remember {
                    when (val it = AppConfig.galleryOptions) {
                        is AppConfig.Gallery.FixColumnCount -> StaggeredGridCells.Fixed(it.size)
                        is AppConfig.Gallery.FixWidth -> StaggeredGridCells.Adaptive(it.size.dp)
                    }
                }
                LazyVerticalStaggeredGrid(
                    columns = columns,
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    state = scroll
                ) {
                    items(
                        state.illusts,
                        key = { it.id }
                    ) {
                        Box(modifier = Modifier.padding(5.dp).clickable {
                            nav.push(IllustDetailScreen(it))
                        }) {
                            Card(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                ProgressedAsyncImage(
                                    url = it.imageUrls.content,
                                    modifier = Modifier.fillMaxWidth()
                                        .aspectRatio(it.width.toFloat() / it.height.toFloat())
                                )
                            }

                            Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp)) {
                                if (it.isR18) {
                                    if (it.isR18G) {
                                        Icon(
                                            modifier = Modifier.padding(end = 4.dp),
                                            imageVector = R18G,
                                            contentDescription = null,
                                            tint = Color.Red
                                        )
                                    }
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        imageVector = R18,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                                if (it.isAI) {
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        imageVector = Robot,
                                        contentDescription = null,
                                        tint = Color.Yellow
                                    )
                                }
                            }

                            var betterFavoriteDialog by remember {
                                mutableStateOf(false)
                            }
                            if (betterFavoriteDialog) {
                                TagFavoriteDialog(
                                    tags = it.tags,
                                    title = { Text("高级收藏设置") },
                                    confirm = { tags, publicity ->
                                        model.likeIllust(it, publicity, tags).join()
                                        betterFavoriteDialog = false
                                    },
                                    cancel = {
                                        betterFavoriteDialog = false
                                    }
                                )
                            }

                            FavoriteButton(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                isFavorite = it.isBookMarked,
                                onModify = { target ->
                                    if (target == FavoriteState.Favorite) {
                                        model.likeIllust(it).join()
                                    } else {
                                        model.disLikeIllust(it).join()
                                    }
                                },
                                onDoubleClick = {
                                    betterFavoriteDialog = true
                                }
                            )
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LaunchedEffect(Unit) {
                            if (!state.noMoreData) {
                                model.loadMoreIllusts()
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

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scroll),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp)
                )

                BackToTopOrRefreshButton(
                    isNotInTop = scroll.canScrollBackward,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    onBackToTop = {
                        scroll.animateScrollToItem(0)
                    },
                    onRefresh = {
                        isRefresh = true
                        model.initIllust(true).invokeOnCompletion {
                            isRefresh = false
                        }
                    }
                )
            }
        }
    }
}
