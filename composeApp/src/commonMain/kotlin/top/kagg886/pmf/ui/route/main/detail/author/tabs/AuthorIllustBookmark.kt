package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustResult
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserLikeIllust
import top.kagg886.pixko.module.user.getUserLikeIllustNext
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import kotlin.coroutines.CoroutineContext

@Composable
fun AuthorScreen.AuthorIllustBookmark(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_illust_bookmark_${user.user.id}") {
        AuthorIllustBookmarkViewModel(user.user.id)
    }
    IllustFetchScreen(model)
}

private class AuthorIllustBookmarkViewModel(val user: Int) : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>() {
            private var context : IllustResult? = null
            override suspend fun onFetchList(): List<Illust>? {
                val result = kotlin.runCatching {
                    context = if (context == null) {
                        client.getUserLikeIllust(user)
                    } else {
                        client.getUserLikeIllustNext(context!!)
                    }
                }
                if (result.isFailure) {
                    return null
                }
                return context!!.illusts
            }

        }
    }

}