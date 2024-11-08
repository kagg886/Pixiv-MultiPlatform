package top.kagg886.pmf.ui.route.main.detail.illust

import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pixko.module.illust.getIllustComment
import top.kagg886.pixko.module.illust.getIllustCommentReply
import top.kagg886.pixko.module.illust.sendIllustComment
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.CommentViewModel


class IllustCommentViewModel(id: Long) : CommentViewModel(id) {
    private val client = PixivConfig.newAccountFromConfig()

    override suspend fun fetchComments(id: Long, page: Int): List<Comment> = client.getIllustComment(id, page)

    override suspend fun fetchCommentReply(commentId: Long): InfinityRepository<Comment> =
        object : InfinityRepository<Comment>() {
            private var page = 1
            override suspend fun onFetchList(): List<Comment> {
                return client.getIllustCommentReply(commentId,page).apply {
                    page += 1
                }
            }
        }

    override suspend fun sendComment(parentId: Long?, id: Long, text: String) {
        client.sendIllustComment {
            parentCommentId = parentId
            illustId = id
            comment = text
        }
    }

}