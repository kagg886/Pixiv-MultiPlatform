package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pmf.backend.pixiv.InfinityRepository

abstract class CommentViewModel(private val id: Long) : ContainerHost<CommentViewState, CommentSideEffect>, ViewModel(),
    KoinComponent, ScreenModel {
    override val container: Container<CommentViewState, CommentSideEffect> = container(CommentViewState.Loading) {
        load()
    }

    private var repo: InfinityRepository<Comment>? = null
    private var replyRepo: InfinityRepository<Comment>? = null

    private val scope = viewModelScope + Dispatchers.IO

    abstract suspend fun fetchComments(id: Long, page: Int): List<Comment>
    abstract suspend fun fetchCommentReply(commentId: Long): InfinityRepository<Comment>
    abstract suspend fun sendComment(parentId: Long?, id: Long, text: String)

    fun load(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                CommentViewState.Loading
            }
        }
        repo = object : InfinityRepository<Comment>(scope.coroutineContext) {
            var page: Int = 1
            override suspend fun onFetchList(): List<Comment> {
                return fetchComments(id, page).apply {
                    page += 1
                }
            }
        }
        reduce {
            CommentViewState.Success.Generic(repo!!.take(20).toList(), repo!!.noMoreData)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMore() = intent {
        runOn<CommentViewState.Success.Generic> {
            reduce {
                state.copy(
                    comments = state.comments + repo!!.take(20).toList(),
                )
            }
        }

        runOn<CommentViewState.Success.HasReply> {
            reduce {
                state.copy(
                    comments = state.comments + repo!!.take(20).toList(),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun clearReply() = intent {
        runOn<CommentViewState.Success.HasReply> {
            replyRepo = null
            reduce {
                CommentViewState.Success.Generic(
                    state.comments,
                    state.noMoreData,
                    state.scrollerState
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadReply(comment: Comment) = intent {
        runOn<CommentViewState.Success.HasReply> {
            clearReply().join()
        }
        runOn<CommentViewState.Success.Generic> {
            replyRepo = fetchCommentReply(comment.id)
            reduce {
                CommentViewState.Success.HasReply(
                    state.comments,
                    state.noMoreData,
                    state.scrollerState,
                    replyRepo!!.take(20).toList(),
                    replyRepo!!.noMoreData,
                    comment
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadReplyMore() = intent {
        runOn<CommentViewState.Success.HasReply> {
            val l = state.replyList ?: emptyList()
            reduce {
                state.copy(
                    replyList = l + repo!!.take(20).toList(),
                    replyNoMoreData = state.replyNoMoreData
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun sendComment(text: String) = intent {
        runOn<CommentViewState.Success> {
            val result = kotlin.runCatching {
                sendComment((state as? CommentViewState.Success.HasReply)?.replyTarget?.id, id, text)
            }
            if (result.isSuccess) {
                postSideEffect(CommentSideEffect.Toast("评论成功"))
                load(true).join()
                return@runOn
            }
            postSideEffect(CommentSideEffect.Toast("评论失败"))
        }
    }
}

sealed interface CommentViewState {
    data object Loading : CommentViewState
    sealed interface Success : CommentViewState {
        val comments: List<Comment>
        val noMoreData: Boolean
        val scrollerState: LazyListState

        data class Generic(
            override val comments: List<Comment>,
            override val noMoreData: Boolean,
            override val scrollerState: LazyListState = LazyListState()
        ) : Success

        data class HasReply(
            override val comments: List<Comment>,
            override val noMoreData: Boolean,
            override val scrollerState: LazyListState,


            val replyList: List<Comment>,
            val replyNoMoreData: Boolean,
            val replyTarget: Comment,
        ) : Success
    }
}

sealed class CommentSideEffect {
    data class Toast(val msg: String) : CommentSideEffect()
}
