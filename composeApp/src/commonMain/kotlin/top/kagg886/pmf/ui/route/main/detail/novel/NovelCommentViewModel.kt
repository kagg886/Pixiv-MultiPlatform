package top.kagg886.pmf.ui.route.main.detail.novel

import top.kagg886.pixko.module.illust.Comment
import top.kagg886.pixko.module.novel.getNovelComment
import top.kagg886.pixko.module.novel.getNovelCommentReply
import top.kagg886.pixko.module.novel.sendNovelComment
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.backend.pixiv.PixivConfig
import top.kagg886.pmf.ui.util.CommentViewModel

class NovelCommentViewModel(id: Long) : CommentViewModel(id) {
    val client = PixivConfig.newAccountFromConfig()
    override suspend fun fetchComments(id: Long, page: Int): List<Comment> = client.getNovelComment(id, page)

    override suspend fun fetchCommentReply(commentId: Long): InfinityRepository<Comment> = object : InfinityRepository<Comment>() {
        var page = 1
        override suspend fun onFetchList(): List<Comment> = client.getNovelCommentReply(commentId, page).apply {
            page += 1
        }
    }

    override suspend fun sendComment(parentId: Long?, id: Long, text: String) {
        client.sendNovelComment {
            novelId = id
            parentCommentId = parentId
            comment = text
        }
    }
}
