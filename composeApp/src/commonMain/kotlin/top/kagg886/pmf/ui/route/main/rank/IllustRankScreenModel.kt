package top.kagg886.pmf.ui.route.main.rank

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.illust.RankCategory
import top.kagg886.pixko.module.illust.getRankIllust
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.catch
import top.kagg886.pmf.ui.util.page

class IllustRankScreenModel(val type: RankCategory) : IllustFetchViewModel() {
    override fun source() = Pager(PagingConfig(pageSize = 30)) {
        object : PagingSource<Int, Illust>() {
            override fun getRefreshKey(state: PagingState<Int, Illust>) = null
            override suspend fun load(params: LoadParams<Int>) = catch {
                params.page { i -> client.getRankIllust(mode = type, page = i) }
            }
        }
    }.flow
}
