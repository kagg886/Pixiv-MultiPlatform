package top.kagg886.pmf.ui.route.main.space

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.IllustResult
import top.kagg886.pixko.module.illust.getLatestIllust
import top.kagg886.pixko.module.illust.getLatestIllustNext
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class NewestIllustViewModel : IllustFetchViewModel() {
    override val rawSource = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<IllustResult, Illust>() {
            override fun getRefreshKey(state: PagingState<IllustResult, Illust>) = null
            override suspend fun load(params: LoadParams<IllustResult>) = withContext(Dispatchers.IO) {
                val result = params.key?.let { ctx -> client.getLatestIllustNext(ctx) } ?: client.getLatestIllust()
                LoadResult.Page(result.illusts, null, result)
            }
        }
    }.flow
}
