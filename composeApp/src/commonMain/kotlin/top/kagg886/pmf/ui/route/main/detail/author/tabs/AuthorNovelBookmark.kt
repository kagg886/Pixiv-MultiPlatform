package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.illust.NovelResult
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserLikeNovel
import top.kagg886.pixko.module.user.getUserLikeNovelNext
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.NovelFetchScreen
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.catch
import top.kagg886.pmf.ui.util.next

@Composable
fun AuthorScreen.AuthorNovelBookmark(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_novel_bookmark_${user.user.id}") {
        AuthorNovelBookmarkViewModel(user.user.id)
    }
    NovelFetchScreen(model)
}

private class AuthorNovelBookmarkViewModel(val user: Int) : NovelFetchViewModel() {
    override fun source() = Pager(PagingConfig(20)) {
        object : PagingSource<NovelResult, Novel>() {
            override fun getRefreshKey(state: PagingState<NovelResult, Novel>) = null
            override suspend fun load(params: LoadParams<NovelResult>) = catch {
                params.next(
                    { client.getUserLikeNovel(user) },
                    { ctx -> client.getUserLikeNovelNext(ctx) },
                    { ctx -> ctx.novels },
                )
            }
        }
    }.flow
}
