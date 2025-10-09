package top.kagg886.pmf.ui.util

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.pixko.module.user.TagFilter
import top.kagg886.pmf.res.*
import top.kagg886.pmf.ui.component.BackToTopOrRefreshButton
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.util.stringResource

@Composable
fun TagsFetchDrawerSheetContainer(model: TagsFetchViewModel, preview: (@Composable () -> Unit)? = null) {
    val state by model.collectAsState()
    TagsFetchContent0(state, model, preview)
}

@Composable
private fun TagsFetchContent0(
    state: TagsFetchViewState,
    model: TagsFetchViewModel,
    preview: (@Composable () -> Unit)? = null,
) {
    val data = model.data.collectAsLazyPagingItems()
    when {
        !data.loadState.isIdle && data.itemCount == 0 -> Loading()
        else -> {
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
                    scope.launch {
                        isRefresh = true
                        model.refresh()
                        data.awaitNextState()
                        isRefresh = false
                    }
                },
            ) {
                if (data.itemCount == 0 && data.loadState.isIdle) {
                    ErrorPage(text = stringResource(Res.string.page_is_empty)) {
                        data.retry()
                    }
                    return@PullToRefreshBox
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    state = state.scrollerState,
                ) {
                    preview?.let {
                        item(key = "Preview") {
                            it()
                        }
                    }
                    item(key = "All") {
                        NavigationDrawerItem(
                            label = { Text(text = stringResource(Res.string.all)) },
                            selected = state.selectedTagsFilter == TagFilter.NoFilter,
                            onClick = { model.clearTags() },
                        )
                    }
                    item(key = "NoFilter") {
                        NavigationDrawerItem(
                            label = { Text(text = stringResource(Res.string.no_filter)) },
                            selected = state.selectedTagsFilter == TagFilter.FilterWithoutTagged,
                            onClick = { model.selectNonTargetTags() },
                        )
                    }
                    items(
                        count = data.itemCount,
                        key = { i -> data.peek(i)!!.name },
                    ) { i ->
                        val item = data[i]!!
                        NavigationDrawerItem(
                            label = { Text(text = item.name) },
                            selected = (state.selectedTagsFilter as? TagFilter.FilterWithTag)?.tag?.name == item.name,
                            icon = { Text(item.count.toString()) },
                            onClick = { model.selectTags(item) },
                        )
                    }
                    item(key = "Footer") {
                        if (!data.loadState.isIdle) {
                            Loading()
                        } else {
                            Text(
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.no_more_data),
                            )
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scroll),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp),
                )

                BackToTopOrRefreshButton(
                    isNotInTop = scroll.canScrollBackward,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onBackToTop = { scroll.animateScrollToItem(0) },
                    onRefresh = {
                        isRefresh = true
                        model.refresh()
                        data.awaitNextState()
                        isRefresh = false
                    },
                )
            }
        }
    }
}
