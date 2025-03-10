package top.kagg886.pmf.ui.route.main.search

import top.kagg886.pixko.module.novel.Novel
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.searchNovel
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import kotlin.coroutines.CoroutineContext

class SearchResultNovelModel(
    val word: String,
    val searchTarget: SearchTarget,
    val sort: SearchSort
) : NovelFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Novel> {
        return object : InfinityRepository<Novel>(coroutineContext) {
            private var page1 = 0
            override suspend fun onFetchList(): List<Novel> {
                val list = client.searchNovel(word) {
                    searchTarget = this@SearchResultNovelModel.searchTarget
                    sort = this@SearchResultNovelModel.sort
                    page = page1 + 1
                }
                page1++
                return list
            }
        }
    }

}
