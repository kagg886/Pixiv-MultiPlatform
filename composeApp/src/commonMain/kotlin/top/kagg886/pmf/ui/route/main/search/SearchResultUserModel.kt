package top.kagg886.pmf.ui.route.main.search

import top.kagg886.pixko.User
import top.kagg886.pixko.module.search.searchUser
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.AuthorFetchViewModel

class SearchResultUserModel(
    private val user: String,
) : AuthorFetchViewModel() {
    override fun initInfinityRepository(): InfinityRepository<User> = object : InfinityRepository<User>() {
        private var page = 1
        override suspend fun onFetchList(): List<User> {
            val result = client.searchUser(keyword = user, page = page)
            page++
            return result
        }
    }
}
