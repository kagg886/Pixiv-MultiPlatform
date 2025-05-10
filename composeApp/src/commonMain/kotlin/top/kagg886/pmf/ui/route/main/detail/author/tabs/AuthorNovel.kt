package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserNovel
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.catch
import top.kagg886.pmf.ui.util.page

@Composable
fun AuthorScreen.AuthorNovel(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_novel_${user.user.id}") {
        AuthorNovelViewModel(user.user.id.toLong())
    }
    NovelFetchScreen(model)
}

class AuthorNovelViewModel(val id: Long) : NovelFetchViewModel() {
    override fun source() = Pager(PagingConfig(20)) {
        object : PagingSource<Int, Novel>() {
            override fun getRefreshKey(state: PagingState<Int, Novel>) = null
            override suspend fun load(params: LoadParams<Int>) = catch {
                params.page { i -> client.getUserNovel(id, i) }
            }
        }
    }.flow
}
