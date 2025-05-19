package top.kagg886.pmf.ui.route.main.detail.illust

import top.kagg886.pixko.module.illust.getIllustComment
import top.kagg886.pixko.module.illust.getIllustCommentReply
import top.kagg886.pixko.module.illust.sendIllustComment
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.CommentViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class IllustCommentViewModel(id: Long) : CommentViewModel(id) {
    private val client = PixivConfig.newAccountFromConfig()
    override fun source(id: Long) = flowOf(30) { p -> p.page { i -> client.getIllustComment(id, i) } }
    override fun reply(id: Long) = flowOf(30) { p -> p.page { i -> client.getIllustCommentReply(id, i) } }

    override suspend fun sendComment(parentId: Long?, id: Long, text: String) {
        client.sendIllustComment {
            parentCommentId = parentId
            illustId = id
            comment = text
        }
    }
}
