package top.kagg886.pmf.ui.route.main.series.novel

import top.kagg886.pixko.module.novel.getNovelSeries
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class NovelSeriesFetchModel(private val id: Int) : NovelFetchViewModel() {
    override fun source() = flowOf(20) { params ->
        params.page { i -> client.getNovelSeries(id, i).novels }
    }
}
