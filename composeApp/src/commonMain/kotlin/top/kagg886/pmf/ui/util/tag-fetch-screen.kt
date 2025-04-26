package top.kagg886.pmf.ui.util

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.kagg886.pixko.module.user.TagFilter
import top.kagg886.pmf.ui.component.BackToTopOrRefreshButton
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter

@Composable
fun TagsFetchDrawerSheetContainer(model: TagsFetchViewModel, preview: (@Composable () -> Unit)? = null) {
    val state by model.collectAsState()
    TagsFetchContent0(state, model, preview)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsFetchContent0(
    state: TagsFetchViewState,
    model: TagsFetchViewModel,
    preview: (@Composable () -> Unit)? = null,
) {
    when (state) {
        TagsFetchViewState.Loading -> {
            Loading()
        }

        is TagsFetchViewState.ShowTagsList -> {
            val scroll = state.scrollerState
            var isRefresh by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

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
                        model.initTags(true).join()
                    }.invokeOnCompletion {
                        isRefresh = false
                    }
                },
            ) {
                if (state.data.isEmpty()) {
                    ErrorPage(text = "页面为空") {
                        scope.launch {
                            model.initTags()
                        }
                    }
                    return@PullToRefreshBox
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    state = state.scrollerState,
                ) {
                    preview?.let {
                        item {
                            it()
                        }
                    }
                    item {
                        NavigationDrawerItem(
                            label = {
                                Text(text = "全部")
                            },
                            selected = state.selectedTagsFilter == TagFilter.NoFilter,
                            onClick = {
                                model.clearTags()
                            },
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = {
                                Text(text = "未分类")
                            },
                            selected = state.selectedTagsFilter == TagFilter.FilterWithoutTagged,
                            onClick = {
                                model.selectNonTargetTags()
                            },
                        )
                    }
                    items(state.data, key = { it.name.hashCode() }) {
                        NavigationDrawerItem(
                            label = {
                                Text(text = it.name)
                            },
                            selected = (state.selectedTagsFilter as? TagFilter.FilterWithTag)?.tag?.name == it.name,
                            icon = {
                                Text(it.count.toString())
                            },
                            onClick = {
                                model.selectTags(it)
                            },
                        )
                    }
                    item {
                        LaunchedEffect(Unit) {
                            if (!state.noMoreData) {
                                model.loadMoreTags()
                            }
                        }
                        if (!state.noMoreData) {
                            Loading()
                            return@item
                        }
                        Text(
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            text = "没有更多了",
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
                            model.initTags(true).join()
                        }.invokeOnCompletion {
                            isRefresh = false
                        }
                    },
                )
            }
        }
    }
}
