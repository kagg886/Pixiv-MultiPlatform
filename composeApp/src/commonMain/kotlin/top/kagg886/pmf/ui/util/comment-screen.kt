package top.kagg886.pmf.ui.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.pmf.LocalSnackBarHost
import top.kagg886.pmf.Res
import top.kagg886.pmf.comment
import top.kagg886.pmf.no_more_data
import top.kagg886.pmf.page_is_empty
import top.kagg886.pmf.reply_for
import top.kagg886.pmf.ui.component.BackToTopOrRefreshButton
import top.kagg886.pmf.ui.component.ErrorPage
import top.kagg886.pmf.ui.component.FavoriteButton
import top.kagg886.pmf.ui.component.Loading
import top.kagg886.pmf.ui.component.scroll.VerticalScrollbar
import top.kagg886.pmf.ui.component.scroll.rememberScrollbarAdapter
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.util.stringResource
import top.kagg886.pmf.util.toReadableString

@Composable
fun CommentPanel(model: CommentViewModel, modifier: Modifier = Modifier) {
    val state by model.collectAsState()
    CommentPanelContainer(model, state, modifier)
}

@Composable
private fun CommentPanelContainer(model: CommentViewModel, state: CommentViewState, modifier: Modifier) {
    val data = model.data.collectAsLazyPagingItems()
    when {
        !data.loadState.isIdle && data.itemCount == 0 -> Loading()
        state is CommentViewState.Success -> {
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
            Column(modifier) {
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
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    if (data.itemCount == 0 && data.loadState.isIdle) {
                        ErrorPage(text = stringResource(Res.string.page_is_empty)) {
                            data.retry()
                        }
                        return@PullToRefreshBox
                    }

                    val controller = remember {
                        keyboardScrollerController(scroll) {
                            scroll.layoutInfo.viewportSize.height.toFloat()
                        }
                    }

                    KeyListenerFromGlobalPipe(controller)

                    LazyColumn(state = scroll, modifier = Modifier.padding(end = 8.dp)) {
                        items(
                            count = data.itemCount,
                            key = { i -> data.peek(i)!!.id },
                        ) { i ->
                            val comment = data[i]!!
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().padding(5.dp),
                            ) {
                                val nav = LocalNavigator.currentOrThrow
                                ListItem(
                                    overlineContent = {
                                        Text(comment.date.toReadableString())
                                    },
                                    headlineContent = {
                                        Text(comment.user.name, style = MaterialTheme.typography.labelSmall)
                                    },
                                    leadingContent = {
                                        AsyncImage(
                                            model = comment.user.profileImageUrls.content,
                                            modifier = Modifier.size(35.dp).clickable { nav.push(AuthorScreen(comment.user.id)) },
                                            contentDescription = null,
                                        )
                                    },
                                    trailingContent = {
                                        AnimatedContent(
                                            targetState = when {
                                                comment.hasReplies -> -1
                                                comment == (state as? CommentViewState.Success.HasReply)?.target -> 1
                                                else -> 0
                                            },
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        ) {
                                            when (it) {
                                                -1 -> FavoriteButton(
                                                    isFavorite = false,
                                                    nonFavoriteIcon = {
                                                        Icon(Icons.Default.MoreVert, null)
                                                    },
                                                ) {
                                                    model.loadReply(comment).join()
                                                }

                                                0 -> FavoriteButton(
                                                    isFavorite = false,
                                                    nonFavoriteIcon = {
                                                        Icon(Icons.Default.Edit, null)
                                                    },
                                                ) {
                                                    model.loadReply(comment).join()
                                                }

                                                1 -> FavoriteButton(
                                                    isFavorite = false,
                                                    nonFavoriteIcon = {
                                                        Icon(Icons.Default.Close, null)
                                                    },
                                                ) {
                                                    model.clearReply().join()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Box(modifier = Modifier.padding(5.dp)) {
                                    if (comment.stamp == null) {
                                        Text(comment.comment)
                                    } else {
                                        AsyncImage(
                                            model = comment.stamp!!.url,
                                            modifier = Modifier.size(100.dp),
                                            contentDescription = null,
                                        )
                                    }
                                }

                                if (state is CommentViewState.Success.HasReply && state.target.id == comment.id) {
                                    val reply = state.reply.collectAsLazyPagingItems()
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        for (i in 0..<reply.itemCount) {
                                            val item = reply[i]!!
                                            ListItem(
                                                headlineContent = {
                                                    Text(item.user.name, style = MaterialTheme.typography.labelSmall)
                                                },
                                                leadingContent = {
                                                    AsyncImage(
                                                        model = item.user.profileImageUrls.content,
                                                        modifier = Modifier.size(25.dp).clickable { nav.push(AuthorScreen(item.user.id)) },
                                                        contentDescription = null,
                                                    )
                                                },
                                                supportingContent = {
                                                    if (item.stamp == null) {
                                                        Text(item.comment)
                                                    } else {
                                                        AsyncImage(
                                                            model = item.stamp!!.url,
                                                            modifier = Modifier.size(80.dp),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                        if (!data.loadState.isIdle) {
                                            Loading()
                                        } else {
                                            Text(
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(ButtonDefaults.TextButtonContentPadding),
                                                text = stringResource(Res.string.no_more_data),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
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

                var text by remember {
                    mutableStateOf("")
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    label = {
                        AnimatedContent(
                            state,
                            transitionSpec = {
                                (fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)) togetherWith (
                                    fadeOut() + slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up,
                                    )
                                    )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            when (it) {
                                is CommentViewState.Success.HasReply -> {
                                    Text(stringResource(Res.string.reply_for, it.target.user.name))
                                }

                                else -> {
                                    Text(stringResource(Res.string.comment))
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        AnimatedVisibility((state as? CommentViewState.Success.HasReply)?.target != null) {
                            IconButton(
                                onClick = {
                                    model.clearReply()
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                            }
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                model.sendComment(text)
                            },
                            enabled = text.isNotBlank(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    },
                )
            }
        }
    }
}
