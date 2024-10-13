package top.kagg886.pmf.ui.route.main.detail.illust

import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.model.ScreenModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import top.kagg886.pixko.PixivAccountFactory
import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pixko.module.illust.getIllustCommentReply
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.backend.pixiv.PixivTokenStorage
import top.kagg886.pmf.ui.util.container


class IllustCommentReplyViewModel(private val commentId: Long, private val page: Int = 1) :
    ContainerHost<IllustCommentReplyState, IllustCommentReplySideEffect>, ViewModel(), KoinComponent,
    ScreenModel {
    override val container: Container<IllustCommentReplyState, IllustCommentReplySideEffect> =
        container(IllustCommentReplyState.Loading) {
            val s = kotlin.runCatching {
                client.getIllustCommentReply(commentId, page)
            }
            if (s.isFailure || s.getOrNull() == null) {
                reduce {
                    IllustCommentReplyState.LoadSuccess(emptyList())
                }
            }
            reduce {
                IllustCommentReplyState.LoadSuccess(s.getOrThrow())
            }
        }

    private val client = PixivConfig.newAccountFromConfig()
}

sealed class IllustCommentReplyState {
    data object Loading : IllustCommentReplyState()
    data class LoadSuccess(val data: List<Comment>, val page: Int = 1) : IllustCommentReplyState()
}

sealed class IllustCommentReplySideEffect {

}