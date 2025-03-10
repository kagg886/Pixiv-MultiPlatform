package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.illust.NovelResult
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserLikeNovel
import top.kagg886.pixko.module.user.getUserLikeNovelNext
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import kotlin.coroutines.CoroutineContext

@Composable
fun AuthorScreen.AuthorNovelBookmark(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_novel_bookmark_${user.user.id}") {
        AuthorNovelBookmarkViewModel(user.user.id)
    }
    NovelFetchScreen(model)
}

private class AuthorNovelBookmarkViewModel(val user: Int) : NovelFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Novel> {
        return object : InfinityRepository<Novel>() {
            private var context : NovelResult? = null
            override suspend fun onFetchList(): List<Novel> {
                context = if (context == null) {
                    client.getUserLikeNovel(user)
                } else {
                    client.getUserLikeNovelNext(context!!)
                }
                return context!!.novels
            }

        }
    }

}
