package top.kagg886.pmf.ui.route.main.detail.novel

import top.kagg886.pixko.module.novel.getNovelComment
import top.kagg886.pixko.module.novel.getNovelCommentReply
import top.kagg886.pixko.module.novel.sendNovelComment
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.CommentViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class NovelCommentViewModel(id: Long) : CommentViewModel(id) {
    val client = PixivConfig.newAccountFromConfig()
    override fun source(id: Long) = flowOf(30) { p -> p.page { i -> client.getNovelComment(id, i) } }
    override fun reply(id: Long) = flowOf(30) { p -> p.page { i -> client.getNovelCommentReply(id, i) } }

    override suspend fun sendComment(parentId: Long?, id: Long, text: String) {
        client.sendNovelComment {
            novelId = id
            parentCommentId = parentId
            comment = text
        }
    }
}
