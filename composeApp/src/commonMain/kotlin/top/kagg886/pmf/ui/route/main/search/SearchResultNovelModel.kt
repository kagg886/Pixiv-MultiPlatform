package top.kagg886.pmf.ui.route.main.search

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.searchNovel
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.catch
import top.kagg886.pmf.ui.util.page

class SearchResultNovelModel(val word: String, val target: SearchTarget, val vsort: SearchSort) : NovelFetchViewModel() {
    override fun source() = Pager(PagingConfig(20)) {
        object : PagingSource<Int, Novel>() {
            override fun getRefreshKey(state: PagingState<Int, Novel>) = null
            override suspend fun load(params: LoadParams<Int>) = catch {
                params.page { i ->
                    client.searchNovel(word) {
                        searchTarget = target
                        sort = vsort
                        page = i
                    }
                }
            }
        }
    }.flow
}
