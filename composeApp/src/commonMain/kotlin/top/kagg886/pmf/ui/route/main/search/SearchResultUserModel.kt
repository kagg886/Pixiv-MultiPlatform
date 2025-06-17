package top.kagg886.pmf.ui.route.main.search

import top.kagg886.pixko.module.search.searchUser
import top.kagg886.pmf.ui.util.AuthorFetchViewModel
import top.kagg886.pmf.ui.util.flowOf
import top.kagg886.pmf.ui.util.page

class SearchResultUserModel(private val user: String) : AuthorFetchViewModel() {
    override fun source() = flowOf(20) { params ->
        params.page { i -> client.searchUser(keyword = user, page = i) }
    }
}
