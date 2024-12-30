package top.kagg886.pmf.ui.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.ui.component.*
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter

@Composable
fun CommentPanel(model: CommentViewModel, modifier: Modifier = Modifier) {
    val state by model.collectAsState()
    CommentPanelContainer(model, state, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentPanelContainer(model: CommentViewModel, state: CommentViewState, modifier: Modifier) {
    when (state) {
        CommentViewState.Loading -> {
            Loading(modifier)
        }

        is CommentViewState.Success -> {
            val scroll = state.scrollerState
            val host = LocalSnackBarHost.current

            model.collectSideEffect {
                when (it) {
                    is CommentSideEffect.Toast -> {
                        host.showSnackbar(it.msg)
                    }
                }
            }

            val scope = rememberCoroutineScope()
            var isRefresh by remember { mutableStateOf(false) }
            Box(modifier) {
                PullToRefreshBox(
                    isRefreshing = isRefresh,
                    onRefresh = {
                        isRefresh = true
                        scope.launch {
                            model.load(true).join()
                        }.invokeOnCompletion {
                            isRefresh = false
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(bottom = TextFieldDefaults.MinHeight + 5.dp)
                ) {
                    if (state.comments.isEmpty()) {
                        ErrorPage(text = "页面为空") {
                            scope.launch {
                                model.load()
                            }
                        }
                        return@PullToRefreshBox
                    }
                    LazyColumn(state = state.scrollerState, modifier = Modifier.padding(end = 8.dp)) {
                        items(state.comments) { comment ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().padding(5.dp)
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(comment.user.name, style = MaterialTheme.typography.labelSmall)
                                    },
                                    leadingContent = {
                                        ProgressedAsyncImage(
                                            url = comment.user.profileImageUrls.content,
                                            modifier = Modifier.size(35.dp)
                                        )
                                    },
                                    trailingContent = {
                                        if (comment.hasReplies) {
                                            FavoriteButton(
                                                isFavorite = false,
                                                nonFavoriteIcon = {
                                                    Icon(Icons.Default.MoreVert, null)
                                                },
                                            ) {
                                                model.loadReply(comment).join()
                                            }
                                            return@ListItem
                                        }
                                        FavoriteButton(
                                            isFavorite = false,
                                            nonFavoriteIcon = {
                                                Icon(Icons.Default.Edit, null)
                                            },
                                        ) {
                                            model.loadReply(comment).join()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(modifier = Modifier.padding(5.dp)) {
                                    if (comment.stamp == null) {
                                        Text(comment.comment)
                                    } else {
                                        ProgressedAsyncImage(
                                            url = comment.stamp!!.url,
                                            modifier = Modifier.size(100.dp)
                                        )
                                    }
                                }
                                if (state.replyTarget?.id == comment.id) {
                                    val replyList = remember(state.replyList) {
                                        state.replyList ?: emptyList()
                                    }
                                    Column(Modifier.fillMaxWidth()) {
                                        for (i in replyList) {
                                            ListItem(
                                                headlineContent = {
                                                    Text(i.user.name, style = MaterialTheme.typography.labelSmall)
                                                },
                                                leadingContent = {
                                                    ProgressedAsyncImage(
                                                        url = i.user.profileImageUrls.content,
                                                        modifier = Modifier.size(25.dp)
                                                    )
                                                },
                                                supportingContent = {
                                                    if (i.stamp == null) {
                                                        Text(i.comment)
                                                    } else {
                                                        ProgressedAsyncImage(
                                                            url = i.stamp!!.url,
                                                            modifier = Modifier.size(80.dp)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                model.loadReplyMore()
                                            },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Text("加载更多")
                                        }
                                    }
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
                                text = "没有更多了"
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
                            scope.launch {
                                scroll.animateScrollToItem(0)
                            }
                        },
                        onRefresh = {
                            isRefresh = true
                            scope.launch {
                                model.load(pullDown = true).join()
                            }.invokeOnCompletion {
                                isRefresh = false
                            }
                        }
                    )
                }


                var text by remember {
                    mutableStateOf("")
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(5.dp),
                    label = {
                        Text(
                            if (state.replyTarget == null) {
                                "评论"
                            } else {
                                "回复 ${state.replyTarget.user.name} 的评论"
                            }
                        )
                    },
                    leadingIcon = {
                        AnimatedVisibility(state.replyTarget != null) {
                            IconButton(
                                onClick = {
                                    model.clearReply()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                            }
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                model.sendComment(text)
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    }
                )
            }
        }
    }
}