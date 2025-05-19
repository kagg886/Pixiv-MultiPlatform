package top.kagg886.pmf.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.Res
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
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen

@Composable
fun NovelFetchScreen(model: NovelFetchViewModel) {
    val state by model.collectAsState()
    NovelFetchContent0(state, model)
}

@Composable
private fun NovelFetchContent0(state: NovelFetchViewState, model: NovelFetchViewModel) {
    val nav = LocalNavigator.currentOrThrow
    when (state) {
        NovelFetchViewState.Loading -> {
            Loading()
        }

        is NovelFetchViewState.ShowNovelList -> {
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
                        model.initNovel(true).join()
                    }.invokeOnCompletion {
                        isRefresh = false
                    }
                },
                modifier = Modifier
                    .ifThen(x != null) { nestedScrollWorkaround(state.scrollerState, x!!) }
                    .fillMaxSize(),
            ) {
                if (state.novels.isEmpty()) {
                    ErrorPage(text = stringResource(Res.string.page_is_empty)) {
                        scope.launch {
                            model.initNovel()
                        }
                    }
                    return@PullToRefreshBox
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    state = scroll,
                ) {
                    items(state.novels, key = { it.id }) {
                        Column {
                            ListItem(
                                overlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (it.isR18 || it.isR18G) {
                                            Icon(
                                                modifier = Modifier.padding(end = 4.dp),
                                                imageVector = R18,
                                                contentDescription = null,
                                                tint = Color.Red,
                                            )
                                        }
                                        if (it.isR18G) {
                                            Icon(
                                                modifier = Modifier.padding(end = 4.dp),
                                                imageVector = R18G,
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
                                        if (!it.series.isNull) {
                                            Text(it.series.title)
                                        }
                                    }
                                },
                                headlineContent = {
                                    Text(it.title)
                                },
                                supportingContent = {
                                    Text(it.tags.take(20).joinToString(", ") { it.name }, minLines = 3, maxLines = 3)
                                },
                                leadingContent = {
                                    AsyncImage(
                                        model = it.imageUrls.content,
                                        modifier = Modifier.width(70.dp).height(90.dp).padding(8.dp),
                                        contentDescription = null,
                                    )
                                },
                                trailingContent = {
                                    var betterFavoriteDialog by remember {
                                        mutableStateOf(false)
                                    }
                                    if (betterFavoriteDialog) {
                                        TagFavoriteDialog(
                                            tags = it.tags,
                                            title = { Text(stringResource(Res.string.bookmark_extra_options)) },
                                            confirm = { tags, publicity ->
                                                model.likeNovel(it, publicity, tags).join()
                                                betterFavoriteDialog = false
                                            },
                                            cancel = {
                                                betterFavoriteDialog = false
                                            },
                                        )
                                    }
                                    FavoriteButton(
                                        isFavorite = it.isBookmarked,
                                        onModify = { target ->
                                            if (target == FavoriteState.Favorite) {
                                                model.likeNovel(it).join()
                                            } else {
                                                model.disLikeNovel(it).join()
                                            }
                                        },
                                        onDoubleClick = {
                                            betterFavoriteDialog = true
                                        },
                                    )
                                },
                                modifier = Modifier.padding(5.dp).clip(CardDefaults.shape).clickable {
                                    nav.push(NovelDetailScreen(it.id.toLong()))
                                },
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
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onBackToTop = {
                        scope.launch {
                            scroll.animateScrollToItem(0)
                        }
                    },
                    onRefresh = {
                        isRefresh = true
                        scope.launch {
                            model.initNovel(true).join()
                        }.invokeOnCompletion {
                            isRefresh = false
                        }
                    },
                )
            }
        }
    }
}
