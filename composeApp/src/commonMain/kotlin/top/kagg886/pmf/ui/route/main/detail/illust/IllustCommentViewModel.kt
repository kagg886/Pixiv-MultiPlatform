package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pixko.module.illust.getIllustComment
import top.kagg886.pixko.module.illust.sendIllustComment
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container


class IllustCommentViewModel : ContainerHost<IllustDetailCommentViewState, IllustDetailCommentSideEffect>, ViewModel(),
    KoinComponent, ScreenModel {
    override val container: Container<IllustDetailCommentViewState, IllustDetailCommentSideEffect> =
        container(IllustDetailCommentViewState.Loading)
    private var repo: InfinityRepository<Comment>? = null
    private val scope = viewModelScope + Dispatchers.IO

    private val token by inject<PixivTokenStorage>()
    private val client = PixivAccountFactory.newAccountFromConfig {
        storage = token
    }

    fun init(id: Long, pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                IllustDetailCommentViewState.Loading
            }
        }
        repo = object : InfinityRepository<Comment>(scope.coroutineContext) {
            var page: Int = 1
            override suspend fun onFetchList(): List<Comment> {
                return client.getIllustComment(id, page++)
            }
        }
        reduce {
            IllustDetailCommentViewState.Success(id, repo!!.take(20).toList(), repo!!.noMoreData)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMore() = intent {
        runOn<IllustDetailCommentViewState.Success> {
            reduce {
                state.copy(
                    comments = state.comments + repo!!.take(20).toList(),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun sendComment(text: String) = intent {
        runOn<IllustDetailCommentViewState.Success> {
            val result = kotlin.runCatching {
                client.sendIllustComment {
                    this.comment = text
                    this.illustId = state.illustId
                }
            }
            if (result.isSuccess) {
                postSideEffect(IllustDetailCommentSideEffect.Toast("评论成功"))
                init(state.illustId).join()
                return@runOn
            }
            postSideEffect(IllustDetailCommentSideEffect.Toast("评论失败"))
        }
    }
}

sealed class IllustDetailCommentViewState {
    data object Loading : IllustDetailCommentViewState()
    data class Success(
        val illustId: Long,
        val comments: List<Comment>,
        val noMoreData: Boolean,
        val scrollerState: LazyListState = LazyListState()
    ) : IllustDetailCommentViewState()
}

sealed class IllustDetailCommentSideEffect {
    data class Toast(val msg: String) : IllustDetailCommentSideEffect()
}
