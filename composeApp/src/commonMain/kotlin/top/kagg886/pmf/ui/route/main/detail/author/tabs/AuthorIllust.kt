package top.kagg886.pmf.ui.route.main.detail.author.tabs

import androidx.compose.runtime.Composable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cafe.adriel.voyager.core.model.rememberScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.user.UserInfo
import top.kagg886.pixko.module.user.getUserIllust
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
    override val rawSource = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<Int, Illust>() {
            override fun getRefreshKey(state: PagingState<Int, Illust>) = null
            override suspend fun load(params: LoadParams<Int>) = withContext(Dispatchers.IO) {
                val key = params.key ?: 1
                val result = client.getUserIllust(user.toLong(), key)
                LoadResult.Page(result, if (key > 1) key - 1 else null, key + 1)
            }
        }
    }.flow
}
