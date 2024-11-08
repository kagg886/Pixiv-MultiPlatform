package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.Dispatchers
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
            CommentViewState.Success(id, repo!!.take(20).toList(), repo!!.noMoreData)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMore() = intent {
        runOn<CommentViewState.Success> {
            reduce {
                state.copy(
                    comments = state.comments + repo!!.take(20).toList(),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun clearReply() = intent {
        runOn<CommentViewState.Success> {
            replyRepo = null
            reduce {
                state.copy(
                    replyList = null,
                    replyTarget = null
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadReply(comment: Comment) = intent {
        runOn<CommentViewState.Success> {
            replyRepo = fetchCommentReply(comment.id)
            reduce {
                state.copy(
                    replyList = replyRepo!!.take(20).toList(),
                    replyTarget = comment
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadReplyMore() = intent {
        runOn<CommentViewState.Success> {
            val l = state.replyList?: emptyList()
            reduce {
                state.copy(
                    replyList = l + repo!!.take(20).toList(),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun sendComment(text: String) = intent {
        runOn<CommentViewState.Success> {
            val result = kotlin.runCatching {
                sendComment(state.replyTarget?.id, id, text)
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

sealed class CommentViewState {
    data object Loading : CommentViewState()
    data class Success(
        val illustId: Long,
        val comments: List<Comment>,
        val noMoreData: Boolean,
        val scrollerState: LazyListState = LazyListState(),

        val replyList: List<Comment>? = null,
        val replyTarget: Comment? = null
    ) : CommentViewState()
}

sealed class CommentSideEffect {
    data class Toast(val msg: String) : CommentSideEffect()
}
