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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import top.kagg886.pixko.module.user.TagFilter
import top.kagg886.pmf.Res
import top.kagg886.pmf.all
import top.kagg886.pmf.no_filter
import top.kagg886.pmf.no_more_data
import top.kagg886.pmf.page_is_empty
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
                    ErrorPage(text = stringResource(Res.string.page_is_empty)) {
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
                                Text(text = stringResource(Res.string.all))
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
                                Text(text = stringResource(Res.string.no_filter))
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
