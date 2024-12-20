package top.kagg886.pmf.ui.route.main.search

import top.kagg886.pixko.User
import top.kagg886.pixko.module.search.searchUser
import top.kagg886.pmf.backend.pixiv.InfinityRepository
import top.kagg886.pmf.ui.util.AuthorFetchViewModel
import kotlin.coroutines.CoroutineContext

class SearchResultUserModel(
    private val user: String
) : AuthorFetchViewModel() {
    override fun initInfinityRepository(coroutineContext: CoroutineContext): InfinityRepository<User> =
        object : InfinityRepository<User>(coroutineContext) {
            private var page = 1
            override suspend fun onFetchList(): List<User>? {
                val result = kotlin.runCatching {
                    client.searchUser(keyword = user, page = page)
                }
                if (result.isFailure) {
                    return null
                }
                page++
                return result.getOrThrow()
            }
        }

}