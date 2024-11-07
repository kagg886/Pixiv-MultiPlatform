package top.kagg886.pmf.ui.util

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import top.kagg886.pmf.ui.component.icon.Disabled
import top.kagg886.pmf.ui.component.icon.Robot
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
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(AppConfig.defaultGalleryWidth),
                    modifier = Modifier.fillMaxSize(),
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
                                if (it.isR18G) {
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        imageVector = Disabled,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                                if (it.isR18) {
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        imageVector = Icons.Default.Warning,
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

                            FavoriteButton(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                isFavorite = it.isBookMarked,
                                onModify = { target ->
                                    if (target == FavoriteState.Favorite) {
                                        model.likeIllust(it).join()
                                    } else {
                                        model.disLikeIllust(it).join()
                                    }
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
