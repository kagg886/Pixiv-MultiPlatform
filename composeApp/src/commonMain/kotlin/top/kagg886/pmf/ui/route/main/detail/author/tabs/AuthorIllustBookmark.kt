package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cafe.adriel.voyager.core.model.rememberScreenModel
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustResult
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserLikeIllust
import top.kagg886.pixko.module.user.getUserLikeIllustNext
import top.kagg886.pmf.ui.route.main.detail.author.AuthorScreen
import top.kagg886.pmf.ui.util.IllustFetchScreen
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.catch

@Composable
fun AuthorScreen.AuthorIllustBookmark(user: UserInfo) {
    val model = rememberScreenModel(tag = "user_illust_bookmark_${user.user.id}") {
        AuthorIllustBookmarkViewModel(user.user.id)
    }
    IllustFetchScreen(model)
}

private class AuthorIllustBookmarkViewModel(val user: Int) : IllustFetchViewModel() {
    override val rawSource = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<IllustResult, Illust>() {
            override fun getRefreshKey(state: PagingState<IllustResult, Illust>) = null
            override suspend fun load(params: LoadParams<IllustResult>) = catch {
                val result = params.key?.let { ctx -> client.getUserLikeIllustNext(ctx) } ?: client.getUserLikeIllust(user)
                LoadResult.Page(result.illusts, null, result)
            }
        }
    }.flow
}
