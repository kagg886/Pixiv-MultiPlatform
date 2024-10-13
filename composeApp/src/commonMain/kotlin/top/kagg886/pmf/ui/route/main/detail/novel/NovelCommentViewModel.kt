package top.kagg886.pmf.ui.route.main.detail.novel

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
import top.kagg886.pixko.module.novel.getNovelComment
import top.kagg886.pixko.module.novel.sendNovelComment
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container

class NovelCommentViewModel : ContainerHost<NovelDetailCommentViewState, NovelDetailCommentSideEffect>, ViewModel(),
    KoinComponent, ScreenModel {
    override val container: Container<NovelDetailCommentViewState, NovelDetailCommentSideEffect> =
        container(NovelDetailCommentViewState.Loading)
    private var repo: InfinityRepository<Comment>? = null
    private val scope = viewModelScope + Dispatchers.IO

    private val client = PixivConfig.newAccountFromConfig()

    fun init(id: Long, pullDown: Boolean = false) = intent {
        if (!pullDown) {
            reduce {
                NovelDetailCommentViewState.Loading
            }
        }
        repo = object : InfinityRepository<Comment>(scope.coroutineContext) {
            var page: Int = 1
            override suspend fun onFetchList(): List<Comment> {
                return client.getNovelComment(id, page++)
            }
        }
        reduce {
            NovelDetailCommentViewState.Success(id, repo!!.take(20).toList(), repo!!.noMoreData)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun loadMore() = intent {
        runOn<NovelDetailCommentViewState.Success> {
            reduce {
                state.copy(
                    comments = state.comments + repo!!.take(20).toList(),
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun sendComment(text: String) = intent {
        runOn<NovelDetailCommentViewState.Success> {
            val result = kotlin.runCatching {
                client.sendNovelComment {
                    this.comment = text
                    this.novelId = state.novelId
                }
            }
            if (result.isSuccess) {
                postSideEffect(NovelDetailCommentSideEffect.Toast("评论成功"))
                init(state.novelId).join()
                return@runOn
            }
            postSideEffect(NovelDetailCommentSideEffect.Toast("评论失败"))
        }
    }
}

sealed class NovelDetailCommentViewState {
    data object Loading : NovelDetailCommentViewState()
    data class Success(
        val novelId: Long,
        val comments: List<Comment>,
        val noMoreData: Boolean,
        val scrollerState: LazyListState = LazyListState()
    ) : NovelDetailCommentViewState()
}

sealed class NovelDetailCommentSideEffect {
    data class Toast(val msg: String) : NovelDetailCommentSideEffect()
}
