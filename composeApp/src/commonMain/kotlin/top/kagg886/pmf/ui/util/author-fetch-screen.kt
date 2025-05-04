package top.kagg886.pmf.ui.util

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import top.kagg886.pmf.Res
import top.kagg886.pmf.no_more_data
import top.kagg886.pmf.page_is_empty
import top.kagg886.pmf.ui.component.BackToTopOrRefreshButton
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.collapsable.v3.LocalConnectedStateKey
import top.kagg886.pmf.ui.component.collapsable.v3.nestedScrollWorkaround
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter

@Composable
fun AuthorFetchScreen(model: AuthorFetchViewModel) {
    val state by model.collectAsState()
    AuthorFetchContent0(state, model)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthorFetchContent0(state: AuthorFetchViewState, model: AuthorFetchViewModel) {
    when (state) {
        is AuthorFetchViewState.Loading -> {
            Loading()
        }

        is AuthorFetchViewState.ShowAuthorList -> {
            val scroll = state.scrollerState

            val scope = rememberCoroutineScope()

            var refresh by remember {
                mutableStateOf(false)
            }

            val controller = remember {
                keyboardScrollerController(scroll) {
                    scroll.layoutInfo.viewportSize.height.toFloat()
                }
            }

            KeyListenerFromGlobalPipe(controller)

            val x = LocalConnectedStateKey.current

            PullToRefreshBox(
                isRefreshing = refresh,
                onRefresh = {
                    refresh = true
                    scope.launch {
                        model.loading(true).join()
                    }.invokeOnCompletion {
                        refresh = false
                    }
                },
                modifier = Modifier
                    .ifThen(x != null) { nestedScrollWorkaround(state.scrollerState, x!!) }
                    .fillMaxSize(),
            ) {
                if (state.data.isEmpty()) {
                    ErrorPage(text = stringResource(Res.string.page_is_empty)) {
                        scope.launch {
                            model.loading()
                        }
                    }
                    return@PullToRefreshBox
                }
                LazyColumn(state = scroll, modifier = Modifier.padding(end = 8.dp)) {
                    items(state.data, key = { it.id }) {
                        AuthorCard(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                            user = it,
                            onFavoritePrivateClick = {
                                model.followUser(it, true)
                            },
                        ) { isRequestFavorite ->
                            if (isRequestFavorite) {
                                model.followUser(it).join()
                            } else {
                                model.unFollowUser(it).join()
                            }
                        }
                    }

                    item {
                        LaunchedEffect(Unit) {
                            if (!state.noMoreData) {
                                model.loadMore()
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
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(4.dp),
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
                        refresh = true
                        scope.launch {
                            model.loading(true).join()
                        }.invokeOnCompletion {
                            refresh = false
                        }
                    },
                )
            }
        }
    }
}
