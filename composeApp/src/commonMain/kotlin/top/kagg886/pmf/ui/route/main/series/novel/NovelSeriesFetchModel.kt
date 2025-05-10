package top.kagg886.pmf.ui.route.main.series.novel

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.getNovelSeries
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.catch
import top.kagg886.pmf.ui.util.page

class NovelSeriesFetchModel(private val id: Int) : NovelFetchViewModel() {
    override fun source() = Pager(PagingConfig(20)) {
        object : PagingSource<Int, Novel>() {
            override fun getRefreshKey(state: PagingState<Int, Novel>) = null
            override suspend fun load(params: LoadParams<Int>) = catch {
                params.page { i -> client.getNovelSeries(id, i).novels }
            }
        }
    }.flow
}
