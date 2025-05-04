package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pmf.Res
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.comment_failed
import top.kagg886.pmf.comment_success

abstract class CommentViewModel(private val id: Long) : ContainerHost<CommentViewState, CommentSideEffect>, ViewModel(), KoinComponent, ScreenModel {
    override val container: Container<CommentViewState, CommentSideEffect> = container(CommentViewState.Loading) {
        load()
    }

    private lateinit var repo: InfinityRepository<Comment>
    private var replyRepo: InfinityRepository<Comment>? = null

    abstract suspend fun fetchComments(id: Long, page: Int): List<Comment>
    abstract suspend fun fetchCommentReply(commentId: Long): InfinityRepository<Comment>
    abstract suspend fun sendComment(parentId: Long?, id: Long, text: String)

    fun load(pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                CommentViewState.Loading
            }
        }
        repo = object : InfinityRepository<Comment>() {
            var page: Int = 1
            override suspend fun onFetchList(): List<Comment> = fetchComments(id, page).apply {
                page += 1
            }
        }
        val list = repo.take(20).toList()
        reduce { CommentViewState.Success.Generic(list, repo.noMoreData) }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMore() = intent {
        runOn<CommentViewState.Success.Generic> {
            val list = state.comments + repo.take(20).toList()
            reduce { state.copy(comments = list) }
        }

        runOn<CommentViewState.Success.HasReply> {
            val list = state.comments + repo.take(20).toList()
            reduce { state.copy(comments = list) }
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
                    state.scrollerState,
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
            val list = replyRepo!!.take(20).toList()
            reduce {
                CommentViewState.Success.HasReply(
                    state.comments,
                    state.noMoreData,
                    state.scrollerState,
                    list,
                    replyRepo!!.noMoreData,
                    comment,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadReplyMore() = intent {
        runOn<CommentViewState.Success.HasReply> {
            val list = state.replyList + repo.take(20).toList()
            reduce { state.copy(replyList = list, replyNoMoreData = state.replyNoMoreData) }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun sendComment(text: String) = intent {
        runOn<CommentViewState.Success> {
            val result = kotlin.runCatching {
                sendComment((state as? CommentViewState.Success.HasReply)?.replyTarget?.id, id, text)
            }
            if (result.isSuccess) {
                postSideEffect(CommentSideEffect.Toast(getString(Res.string.comment_success)))
                load(true).join()
                return@runOn
            }
            postSideEffect(CommentSideEffect.Toast(getString(Res.string.comment_failed)))
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
            override val scrollerState: LazyListState = LazyListState(),
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
