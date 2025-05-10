package top.kagg886.pmf.ui.route.main.search

import top.kagg886.pixko.module.search.SearchSort
import top.kagg886.pixko.module.search.SearchTarget
import top.kagg886.pixko.module.search.searchIllust
import top.kagg886.pmf.ui.util.IllustFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class SearchResultIllustModel(val word: String, val target: SearchTarget, val vsort: SearchSort) : IllustFetchViewModel() {
    override fun source() = flowOf(30) { params ->
        params.page { i ->
            client.searchIllust(word) {
                searchTarget = target
                sort = vsort
                page = i
            }
        }
    }
}
