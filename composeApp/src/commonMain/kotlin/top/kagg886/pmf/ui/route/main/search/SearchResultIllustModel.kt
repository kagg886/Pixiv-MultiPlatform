package top.kagg886.pmf.ui.route.main.search

import top.kagg886.pixko.module.illust.Illust
import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.searchIllust
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import kotlin.coroutines.CoroutineContext

class SearchResultIllustModel(
    val word: String,
    val searchTarget: SearchTarget,
    val sort: SearchSort
) : IllustFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<Illust> {
        return object : InfinityRepository<Illust>(coroutineContext) {
            private var page1 = 0
            override suspend fun onFetchList(): List<Illust>? {
                val list = kotlin.runCatching {
                    client.searchIllust(word) {
                        searchTarget = this@SearchResultIllustModel.searchTarget
                        sort = this@SearchResultIllustModel.sort
                        page = page1 + 1
                    }
                }
                if (list.isSuccess) {
                    page1 += 1
                }
                return list.getOrNull()
            }
        }
    }

}