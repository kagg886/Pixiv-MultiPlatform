package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlin.coroutines.CoroutineContext
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserIllust
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchViewModel

@Composable
fun AuthorScreen.AuthorIllust(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_illust_${user.user.id}") {
        AuthorIllustViewModel(user.user.id)
    }
    IllustFetchScreen(model)
}

private class AuthorIllustViewModel(val user: Int) : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>() {
            private var page = 1
            override suspend fun onFetchList(): List<Illust> {
                val result = client.getUserIllust(user.toLong(), page)
                page++
                return result
            }
        }
    }
}
