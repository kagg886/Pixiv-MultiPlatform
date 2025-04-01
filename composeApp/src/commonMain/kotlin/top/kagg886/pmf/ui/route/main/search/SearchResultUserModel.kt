package top.kagg886.pmf.ui.route.main.search

import kotlin.coroutines.CoroutineContext
import top.kagg886.pixko.User
import top.kagg886.pixko.module.search.searchUser
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.AuthorFetchViewModel

class SearchResultUserModel(
    private val user: String,
) : AuthorFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<User> = object : InfinityRepository<User>(coroutineContext) {
        private var page = 1
        override suspend fun onFetchList(): List<User> {
            val result = client.searchUser(keyword = user, page = page)
            page++
            return result
        }
    }
}
