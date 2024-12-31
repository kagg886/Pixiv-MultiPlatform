package top.kagg886.pmf.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.dialog.TagFavoriteDialog
import top.kagg886.pmf.ui.component.icon.Disabled
import top.kagg886.pmf.ui.component.icon.Robot
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.detail.novel.NovelDetailScreen

@Composable
fun NovelFetchScreen(model: NovelFetchViewModel) {
    val state by model.collectAsState()
    NovelFetchContent0(state, model)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovelFetchContent0(state: NovelFetchViewState, model: NovelFetchViewModel) {
    val nav = LocalNavigator.currentOrThrow
    when (state) {
        NovelFetchViewState.Loading -> {
            Loading()
        }

        is NovelFetchViewState.ShowNovelList -> {
            val scroll = state.scrollerState

//            LaunchedEffect(refreshState.isRefreshing) {
//                if (refreshState.isRefreshing) {
//                    model.initNovel(true).join()
//                    refreshState.endRefresh()
//                }
//            }
            var isRefresh by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
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
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.novels.isEmpty()) {
                    ErrorPage(text = "页面为空") {
                        scope.launch {
                            model.initNovel()
                        }
                    }
                    return@PullToRefreshBox
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    state = scroll
                ) {
                    items(state.novels, key = { it.id }) {
                        Column {
                            ListItem(
                                overlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (it.isR18 || it.isR18G) {
                                            Icon(
                                                modifier = Modifier.padding(end = 4.dp),
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color.Red
                                            )
                                        }
                                        if (it.isR18G) {
                                            Icon(
                                                modifier = Modifier.padding(end = 4.dp),
                                                imageVector = Disabled,
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
                                    ProgressedAsyncImage(
                                        url = it.imageUrls.content,
                                        contentScale = ContentScale.FillHeight,
                                        modifier = Modifier.widthIn(max = 70.dp).height(90.dp).padding(8.dp)
                                    )
                                },
                                trailingContent = {
                                    var betterFavoriteDialog by remember {
                                        mutableStateOf(false)
                                    }
                                    if (betterFavoriteDialog) {
                                        TagFavoriteDialog(
                                            tags = it.tags,
                                            title = { Text("高级收藏设置") },
                                            confirm = { tags, publicity ->
                                                model.likeNovel(it, publicity, tags).join()
                                                betterFavoriteDialog = false
                                            },
                                            cancel = {
                                                betterFavoriteDialog = false
                                            }
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

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scroll),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp)
                )

                BackToTopOrRefreshButton(
                    isNotInTop = scroll.canScrollBackward,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
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
                    }
                )
            }
        }
    }
}
