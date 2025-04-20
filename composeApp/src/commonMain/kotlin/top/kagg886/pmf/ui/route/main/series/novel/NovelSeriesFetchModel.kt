package top.kagg886.pmf.ui.route.main.series.novel

import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.novel.getNovelSeries
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.NovelFetchViewModel

class NovelSeriesFetchModel(private val id: Int) : NovelFetchViewModel() {
    override fun initInfinityRepository(): InfinityRepository<Novel> {
        return object : InfinityRepository<Novel>() {
            private var page: Int = 1
            override suspend fun onFetchList(): List<Novel> {
                val result = client.getNovelSeries(id, page)
                page++
                return result.novels
            }
        }
    }
}
