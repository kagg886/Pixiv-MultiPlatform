package top.kagg886.pmf.ui.route.main.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.searchIllust
import top.kagg886.pmf.ui.util.IllustFetchViewModel

class SearchResultIllustModel(val word: String, val vsearchTarget: SearchTarget, val vsort: SearchSort) : IllustFetchViewModel() {
    override val rawSource = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<Int, Illust>() {
            override fun getRefreshKey(state: PagingState<Int, Illust>) = null
            override suspend fun load(params: LoadParams<Int>) = withContext(Dispatchers.IO) {
                val key = params.key ?: 1
                val result = client.searchIllust(word) {
                    searchTarget = vsearchTarget
                    sort = vsort
                    page = key
                }
                LoadResult.Page(result, if (key > 1) key - 1 else null, key + 1)
            }
        }
    }.flow
}
