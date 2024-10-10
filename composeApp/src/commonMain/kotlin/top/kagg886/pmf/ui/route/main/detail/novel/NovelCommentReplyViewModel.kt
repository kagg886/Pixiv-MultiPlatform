package top.kagg886.pmf.ui.route.main.detail.novel

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pixko.module.novel.getNovelCommentReply
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container

class NovelCommentReplyViewModel(private val commentId: Long, private val page: Int = 1) :
    ContainerHost<NovelCommentReplyState, NovelCommentReplySideEffect>, ViewModel(), KoinComponent,
    ScreenModel {
    override val container: Container<NovelCommentReplyState, NovelCommentReplySideEffect> =
        container(NovelCommentReplyState.Loading) {
            val s = kotlin.runCatching {
                client.getNovelCommentReply(commentId, page)
            }
            if (s.isFailure || s.getOrNull() == null) {
                reduce {
                    NovelCommentReplyState.LoadSuccess(emptyList())
                }
            }
            reduce {
                NovelCommentReplyState.LoadSuccess(s.getOrThrow())
            }
        }

    private val token by inject<PixivTokenStorage>()
    private val client = PixivAccountFactory.newAccountFromConfig {
        storage = token
    }
}

sealed class NovelCommentReplyState {
    data object Loading : NovelCommentReplyState()
    data class LoadSuccess(val data: List<Comment>, val page: Int = 1) : NovelCommentReplyState()
}

sealed class NovelCommentReplySideEffect {

}