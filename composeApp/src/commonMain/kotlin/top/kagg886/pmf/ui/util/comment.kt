package top.kagg886.pmf.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pmf.Res
import top.kagg886.pmf.comment_failed
import top.kagg886.pmf.comment_success

abstract class CommentViewModel(private val id: Long) : ContainerHost<CommentViewState, CommentSideEffect>, ViewModel(), KoinComponent, ScreenModel {
    override val container: Container<CommentViewState, CommentSideEffect> = container(CommentViewState.Success.Generic())
    abstract fun source(id: Long): Flow<PagingData<Comment>>
    abstract fun reply(id: Long): Flow<PagingData<Comment>>
    abstract suspend fun sendComment(parentId: Long?, id: Long, text: String)

    private val refreshSignal = MutableSharedFlow<Unit>()

    val data = merge(flowOf(Unit), refreshSignal).flatMapLatest { source(id) }.cachedIn(viewModelScope)
    fun refresh() = intent { refreshSignal.emit(Unit) }

    @OptIn(OrbitExperimental::class)
    fun clearReply() = intent {
        runOn<CommentViewState.Success.HasReply> {
            reduce { CommentViewState.Success.Generic(state.scrollerState) }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadReply(comment: Comment) = intent {
        runOn<CommentViewState.Success.HasReply> {
            clearReply().join()
        }
        runOn<CommentViewState.Success.Generic> {
            reduce {
                CommentViewState.Success.HasReply(
                    state.scrollerState,
                    reply(comment.id).cachedIn(viewModelScope),
                    comment,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun sendComment(text: String) = intent {
        runOn<CommentViewState.Success> {
            val result = kotlin.runCatching {
                sendComment((state as? CommentViewState.Success.HasReply)?.target?.id, id, text)
            }
            if (result.isSuccess) {
                postSideEffect(CommentSideEffect.Toast(getString(Res.string.comment_success)))
                refreshSignal.emit(Unit)
                return@runOn
            }
            postSideEffect(CommentSideEffect.Toast(getString(Res.string.comment_failed)))
        }
    }
}

sealed interface CommentViewState {
    sealed interface Success : CommentViewState {
        val scrollerState: LazyListState

        data class Generic(
            override val scrollerState: LazyListState = LazyListState(),
        ) : Success

        data class HasReply(
            override val scrollerState: LazyListState,
            val reply: Flow<PagingData<Comment>>,
            val target: Comment,
        ) : Success
    }
}

sealed class CommentSideEffect {
    data class Toast(val msg: String) : CommentSideEffect()
}
