package top.kagg886.pmf.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.AppConfig
import top.kagg886.pmf.bookmark_extra_options
import top.kagg886.pmf.no_more_data
import top.kagg886.pmf.page_is_empty
import top.kagg886.pmf.ui.component.BackToTopOrRefreshButton
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.FavoriteButton
import top.kagg886.pmf.ui.component.FavoriteState
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.collapsable.v3.LocalConnectedStateKey
import top.kagg886.pmf.ui.component.collapsable.v3.nestedScrollWorkaround
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

            val x = LocalConnectedStateKey.current
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
                modifier = Modifier
                    .ifThen(x != null) { nestedScrollWorkaround(state.scrollerState, x!!) }
                    .fillMaxSize(),
            ) {
                if (state.illusts.isEmpty()) {
                    ErrorPage(text = stringResource(Res.string.page_is_empty)) {
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
                    state = scroll,
                ) {
                    items(
                        state.illusts,
                        key = { it.id },
                    ) {
                        Box(modifier = Modifier.padding(5.dp)) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                onClick = { nav.push(IllustDetailScreen(it)) },
                            ) {
                                AsyncImage(
                                    model = it.imageUrls.content,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(it.width.toFloat() / it.height.toFloat()),
                                    contentDescription = null,
                                )
                            }

                            Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp)) {
                                if (it.isR18) {
                                    if (it.isR18G) {
                                        Icon(
                                            modifier = Modifier.padding(end = 4.dp),
                                            imageVector = R18G,
                                            contentDescription = null,
                                            tint = Color.Red,
                                        )
                                    }
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        imageVector = R18,
                                        contentDescription = null,
                                        tint = Color.Red,
                                    )
                                }
                                if (it.isAI) {
                                    Icon(
                                        modifier = Modifier.padding(end = 4.dp),
                                        imageVector = Robot,
                                        contentDescription = null,
                                        tint = Color.Yellow,
                                    )
                                }
                            }

                            var betterFavoriteDialog by remember {
                                mutableStateOf(false)
                            }
                            if (betterFavoriteDialog) {
                                TagFavoriteDialog(
                                    tags = it.tags,
                                    title = { Text(stringResource(Res.string.bookmark_extra_options)) },
                                    confirm = { tags, publicity ->
                                        model.likeIllust(it, publicity, tags).join()
                                        betterFavoriteDialog = false
                                    },
                                    cancel = {
                                        betterFavoriteDialog = false
                                    },
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
                                },
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
                            text = stringResource(Res.string.no_more_data),
                        )
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scroll),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp),
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
                    },
                )
            }
        }
    }
}
