package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlin.coroutines.CoroutineContext
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserNovel
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchViewModel

@Composable
fun AuthorScreen.AuthorNovel(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_novel_${user.user.id}") {
        AuthorNovelViewModel(user.user.id.toLong())
    }
    NovelFetchScreen(model)
}

class AuthorNovelViewModel(val id: Long) : NovelFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Novel> {
        return object : InfinityRepository<Novel>(coroutineContext) {
            var page = 1
            override suspend fun onFetchList(): List<Novel> {
                val result = client.getUserNovel(id, page)

                page++
                return result
            }
        }
    }
}
