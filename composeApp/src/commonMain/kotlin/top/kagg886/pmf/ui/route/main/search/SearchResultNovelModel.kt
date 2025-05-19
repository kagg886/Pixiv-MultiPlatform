package top.kagg886.pmf.ui.route.main.search

import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.searchNovel
import top.kagg886.pmf.ui.util.NovelFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class SearchResultNovelModel(val word: String, val target: SearchTarget, val vsort: SearchSort) : NovelFetchViewModel() {
    override fun source() = flowOf(20) { params ->
        params.page { i ->
            client.searchNovel(word) {
                searchTarget = target
                sort = vsort
                page = i
            }
        }
    }
}
